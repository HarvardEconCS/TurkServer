package edu.harvard.econcs.turkserver.mturk;

import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.QualificationRequirement;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.google.inject.Inject;

/**
 * A hacked together class to create HITs and expire leftovers when the end
 * has been reached.
 * 
 * posts gradually to keep us at the top of the list.
 * 
 * TODO needs a lot of work.
 * 
 * @author mao
 *
 */
public class TurkHITController implements HITController {

	private static final int HIT_SLEEP_MILLIS = 200;
	private static final int SERVICE_UNAVAILABLE_MILLIS = 5000;

	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	private final RequesterServiceExt requester;
	private final ExperimentDataTracker tracker;
	
	private String hitTypeId;
	private String title;
	
	private String externalURL;
	private int frameHeight;
	private int lifeTime;
			
	private final BlockingQueue<int[]> jobs;
	private volatile boolean expireFlag;
	
	/**
	 * 
	 * @param req
	 * @param tracker
	 * @param initialAmount the amount of hits to initially create
	 * @param additionalDelay the amount to wait before each additional hit
	 * @param totalAmount
	 */
	@Inject
	public TurkHITController(
			RequesterServiceExt req, 
			ExperimentDataTracker tracker
			) {
		this.requester = req;
		this.tracker = tracker;
				
		jobs = new LinkedBlockingQueue<int[]>();
		expireFlag = false;
	}

	/**
	 * Sets the HIT type that this manager will use
	 * @param title
	 * @param description
	 * @param keywords
	 * @param rewardAmountInUSD
	 * @param assignmentDurationInSeconds
	 * @param autoApprovalDelayInSeconds
	 */
	@Override
	public void setHITType(
			String title, 
			String description,
			String keywords,
			double reward,
			long assignmentDurationInSeconds,
			long autoApprovalDelayInSeconds,			
			QualificationRequirement[] qualRequirements) {
		try {					
			hitTypeId = requester.registerHITType(
					autoApprovalDelayInSeconds, assignmentDurationInSeconds, 
					reward, title, keywords, description, qualRequirements);
			this.title = title;
			
			logger.info("Got HIT Type: {}, title is {}", hitTypeId, title);
			
		} catch (ServiceException e) {			
			e.printStackTrace();
		}		
	}
	
	/**
	 * Sets the parameters for external question
	 * @param url
	 * @param frameHeight
	 * @param lifetime
	 */
	@Override
	public void setExternalParams(String url, int frameHeight, int lifetime) {
		this.externalURL = url;
		this.frameHeight = frameHeight;
		this.lifeTime = lifetime;
	}		
	
	@Override
	public void disableAndShutdown() {
		expireFlag = true;
		jobs.offer(new int[] {});		
	}
	
	@Override
	public void postBatchHITs(int initialAmount, int delay, int totalAmount) {
		jobs.offer(new int[] {initialAmount, delay, totalAmount});
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName(this.getClass().getSimpleName());			
		
		while( !expireFlag ) {
			int[] nextJob = null;
			try { nextJob = jobs.take(); }
			catch (InterruptedException e) { e.printStackTrace(); }
			
			if( nextJob == null || nextJob.length == 0 ) continue;
			
			final int initialAmount = nextJob[0];
			final int additionalDelay = nextJob[1];
			final int hitAmount = nextJob[2];
			
			logger.info("Starting HIT creation: up to {} HITs", hitAmount);			
			
			// Create HITs until our limit is reached, or expire			
			for( int i = 0; i < hitAmount; i++ ) {			
				long sleepMillis = i > initialAmount ? additionalDelay : HIT_SLEEP_MILLIS; 
				logger.info("Sleeping for " + sleepMillis);
				
				try { Thread.sleep(sleepMillis); } 
				catch (InterruptedException e1) { e1.printStackTrace();	}
				
				// Quit if expiration was reached while sleeping
				if( expireFlag ) break;
							
				try {
					HIT resp = requester.createHITExternalFromID(
								hitTypeId, title, externalURL, frameHeight, String.valueOf(lifeTime));

					String hitId = resp.getHITId();
					tracker.saveHITId(hitId);				

				} catch (ServiceException e) {
					
					e.printStackTrace();
					i--;

					logger.info("Throttling HIT creating");
					// Throttle it a bit
					try { Thread.sleep(SERVICE_UNAVAILABLE_MILLIS); } 
					catch (InterruptedException e1) { e1.printStackTrace();	}				
				}
			
				logger.info(String.format("Created %d HITs", i));
			}						
		}								
		
		// Wait around until server tells us to expire HITs
		while( !expireFlag ) {
			try { Thread.sleep(2000); } 
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		logger.info("Disabling leftover hits");
		List<Session> unusedHITs = tracker.expireUnusedSessions();
		
		for( Session session : unusedHITs ) {
			String hitId = session.getHitId();									
			requester.safeDisableHIT(hitId);			
			
			try { Thread.sleep(HIT_SLEEP_MILLIS); } 
			catch (InterruptedException e1) { e1.printStackTrace();	}
		}
		
		logger.info("Turk HIT posting thread finished");
	}

}
