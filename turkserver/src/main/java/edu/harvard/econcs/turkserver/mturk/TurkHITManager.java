package edu.harvard.econcs.turkserver.mturk;

import edu.harvard.econcs.turkserver.mturk.response.CreateHITResponse;
import edu.harvard.econcs.turkserver.mturk.response.DisableHITResponse;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.mysql.DataTracker;

import java.math.BigInteger;
import java.util.List;
import java.util.logging.Logger;

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
public class TurkHITManager<T> implements Runnable {

	private static final int HIT_SLEEP_MILLIS = 200;
	private static final int SERVICE_UNAVAILABLE_MILLIS = 5000;

	protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private final MTurkRequester requester;
	private final DataTracker<T> tracker;
	
	private final int initialAmount;
	private final int additionalDelay;
	private final int hitAmount;
	
	private String hitTypeId;
	
	private String externalURL;
	private int frameHeight;
	private int lifeTime;
			
	private volatile boolean expireFlag;
	
	/**
	 * 
	 * @param req
	 * @param tracker
	 * @param initialAmount the amount of hits to initially create
	 * @param additionalDelay the amount to wait before each additional hit
	 * @param totalAmount
	 */
	public TurkHITManager(MTurkRequester req, DataTracker<T> tracker, int initialAmount, 
			int additionalDelay, int totalAmount) {
		this.requester = req;
		this.tracker = tracker;
		
		this.initialAmount = initialAmount;
		this.additionalDelay = additionalDelay;
		this.hitAmount = totalAmount;
				
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
	public void setHITType(
			String title, 
			String description,
			String keywords,
			String rewardAmountInUSD,
			int assignmentDurationInSeconds,
			int autoApprovalDelayInSeconds,			
			List<Qualification> qualifications) {
		try {
			hitTypeId = requester.registerHITTypeWithQual(
					title, description, keywords, rewardAmountInUSD, 
					assignmentDurationInSeconds, autoApprovalDelayInSeconds, 
					qualifications);			
			
			logger.info("Got HIT Type: " + hitTypeId);
			
		} catch (TurkException e) {			
			e.printStackTrace();
		}		
	}
	
	/**
	 * Sets the parameters for external question
	 * @param url
	 * @param frameHeight
	 * @param lifetime
	 */
	public void setExternalParams(String url, int frameHeight, int lifetime) {
		this.externalURL = url;
		this.frameHeight = frameHeight;
		this.lifeTime = lifetime;
	}		
	
	/**
	 * Called by the server to expire all remaining HITs once enough shit has been reached
	 */
	public void expireRemainingHITs() {
		expireFlag = true;
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName(this.getClass().getSimpleName());
		
		// Thread that creates HITs as soon as it is started
		int i;
		
		// Create HITs until our limit is reached, or expire
		for( i = 0; i < hitAmount; i++ ) {			
			long sleepMillis = i > initialAmount ? additionalDelay * 1000 : HIT_SLEEP_MILLIS; 
			logger.info("Sleeping for " + sleepMillis);
			
			try { Thread.sleep(sleepMillis); } 
			catch (InterruptedException e1) { e1.printStackTrace();	}
			
			// Quit if expiration was reached while sleeping
			if( expireFlag ) break;
			
			T newID = tracker.getNewSessionID();
			String newUrl = null;
			
			// Depends if we need to identify each HIT by session or not 
			if( newID != null && newID instanceof BigInteger ) {
				String id = ((BigInteger) newID).toString(16);
				newUrl = externalURL.contains("?") ?
						externalURL + "&id=" + id : 
						externalURL + "?id=" + id;
			}
			else {
				newUrl = externalURL;
			}			

			try {
				CreateHITResponse resp = requester.createHITExternalFromID(
							hitTypeId, newUrl, frameHeight, String.valueOf(lifeTime));

				String hitId = resp.getHITId();
				tracker.saveHITIdForSession(newID, hitId);				

			} catch (TurkException e) {
				e.printStackTrace();
				i--;

				logger.info("Throttling HIT creating");
				// Throttle it a bit
				try { Thread.sleep(SERVICE_UNAVAILABLE_MILLIS); } 
				catch (InterruptedException e1) { e1.printStackTrace();	}				
			}
			
		}
		
		logger.info(String.format("Created %d HITs", i));
		
		// Wait around until server tells us to expire HITs
		while( !expireFlag ) {
			try { Thread.sleep(2000); } 
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		logger.info("Disabling leftover hits");
		List<SessionRecord> unusedHITs = tracker.expireUnusedSessions();
		
		for( SessionRecord session : unusedHITs ) {
			String hitId = session.getHitId();
			// TODO fix this ugly ass code
			do {			
				try {
					DisableHITResponse resp = requester.disableHIT(hitId);
					if( !resp.isValid() ) {
						logger.warning("Error disabling HIT " + hitId);
						resp.printXMLResponse();
					}
					
					break;					
				} catch (TurkException e) {					
					e.printStackTrace();
					
					logger.info("Throttling HIT disabling");
					// Throttle it a bit
					try { Thread.sleep(SERVICE_UNAVAILABLE_MILLIS); } 
					catch (InterruptedException e1) { e1.printStackTrace();	}	
					continue;
				}
			} while(true);
			
			try { Thread.sleep(HIT_SLEEP_MILLIS); } 
			catch (InterruptedException e1) { e1.printStackTrace();	}
		}
		
		logger.info("Turk HIT posting thread finished");
	}

}
