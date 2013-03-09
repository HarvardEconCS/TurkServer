package edu.harvard.econcs.turkserver.mturk;

import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker.SessionSummary;

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

	private static final CreateTask POISON_PILL = new CreateTask(0,0,0,0,0);
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	private final RequesterServiceExt requester;
	private final ExperimentDataTracker tracker;
	
	private String hitTypeId;
	private String title;
	
	private String externalURL;
	private int frameHeight;
	private int lifeTime;
			
	private final BlockingQueue<CreateTask> jobs;
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
				
		jobs = new LinkedBlockingQueue<CreateTask>();
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
		jobs.offer(POISON_PILL);		
	}
	
	@Override
	public void postBatchHITs(int target, int minOverhead, int maxOverhead, int minDelay, double pctOverhead) {
		jobs.offer(new CreateTask(target, minOverhead, maxOverhead, minDelay, pctOverhead));
	}
	
	static class CreateTask {
		final int target, minOverhead, maxOverhead, minDelay;
		final double pctOverhead;
		public CreateTask(int target, int minOverhead, int maxOverhead, 
				int minDelay, double pctOverhead) {
			this.target = target;
			this.minOverhead = minOverhead;
			this.maxOverhead = maxOverhead;
			this.minDelay = minDelay;
			this.pctOverhead = pctOverhead;
		}	
	}

	@Override
	public void run() {
		Thread.currentThread().setName(this.getClass().getSimpleName());			
		
		while( !expireFlag ) {
			CreateTask nextJob = null;
			try { nextJob = jobs.take(); }
			catch (InterruptedException e) { e.printStackTrace(); }			
			if( nextJob == POISON_PILL ) continue;						
			
			logger.info("Starting HIT creation: max overhead {}, min delay {}", nextJob.maxOverhead, nextJob.minDelay);			
			
			int maxHITs = getAdaptiveTarget(nextJob.target, nextJob);
					
			SessionSummary summary;
			
			// Create HITs until our limit is reached, or expire			
			do {							
				int sleepMillis = Math.max(HIT_SLEEP_MILLIS, nextJob.minDelay);								
				logger.info("Sleeping for " + sleepMillis);
				
				try { Thread.sleep(sleepMillis); } 
				catch (InterruptedException e) { e.printStackTrace();	}
				
				// Quit if expiration was reached while sleeping
				if( expireFlag ) break;								
				
				summary = tracker.getSetSessionSummary();
				
				int target = getAdaptiveTarget(summary.assignedHITs, nextJob);
				if( summary.createdHITs >= target ) continue;
							
				try {
					HIT resp = requester.createHITExternalFromID(
								hitTypeId, title, externalURL, frameHeight, String.valueOf(lifeTime));

					String hitId = resp.getHITId();
					tracker.saveHITId(hitId);
					
					logger.info(String.format("Created %d HITs", summary.createdHITs + 1));
				} catch (ServiceException e) {					
					e.printStackTrace();					

					logger.info("Got error; Throttling HIT creating");
					// Throttle it a bit
					try { Thread.sleep(SERVICE_UNAVAILABLE_MILLIS); } 
					catch (InterruptedException e1) { e1.printStackTrace();	}				
				}							
			} while (summary.createdHITs < maxHITs);		
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

	private static int getAdaptiveTarget(int target, CreateTask nextJob) {
		int adjusted = (int) Math.round(target * (1 + nextJob.pctOverhead));
		adjusted = Math.max(adjusted, target + nextJob.minOverhead);
		adjusted = Math.min(adjusted, target + nextJob.maxOverhead);
		return adjusted;
	}

}
