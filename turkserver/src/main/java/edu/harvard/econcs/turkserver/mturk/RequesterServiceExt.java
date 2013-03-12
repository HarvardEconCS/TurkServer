package edu.harvard.econcs.turkserver.mturk;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ObjectDoesNotExistException;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.ClientConfig;
import com.google.inject.Inject;

/**
 * Class for connecting to MTurk and submitting REST requests.
 * @author mao
 *
 */
public class RequesterServiceExt extends RequesterService {
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	public static final int THROTTLE_SLEEP_TIME = 2000;
	public static final int MAX_RETRIES = 3;
	
	@Inject
	public RequesterServiceExt(ClientConfig config) {
		super(config);	
	}

	/**
	 * Creates a new HIT using an external question
	 * @param hitTypeID
	 * @param url
	 * @param frameHeight
	 * @param lifetimeInSeconds
	 * @return
	 * @throws IOException
	 */
	public HIT createHITExternalFromID(String hitTypeID, String title, String url, int frameHeight, String lifetimeInSeconds)
	throws ServiceException
	{
		String question = String.format(
				"<ExternalQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd\">" +
				"<ExternalURL>%s</ExternalURL>" +
				"<FrameHeight>%d</FrameHeight>" +
				"</ExternalQuestion>",
				// Need to escape XML entities going into this structure
				StringEscapeUtils.escapeXml(url), frameHeight);		

		// Only HitType, question, and lifetime needed
		return super.createHIT(hitTypeID, title, null, null, question, null, null, null, 
				new Long(lifetimeInSeconds), null, null, null, null);		
	}

	/**
	 * Sends a message to all workers in a list.
	 * 
	 * @param subject
	 * @param message
	 * @param workers
	 * @return
	 * @throws ServiceException
	 */
	public boolean notifyAllWorkers(String subject, String message, 
			List<String> workers, int skipAmount) throws ServiceException {
		Iterator<String> wit = workers.iterator();			
		
		// TODO get rid of this skipping stuff
		int batchSize = 20;
		
		int i = 0;
		while( wit.hasNext() && i < skipAmount ) {
			wit.next(); i++;
		}
		
		System.out.println("Skipped " + i + " workers");
				
		while( wit.hasNext() ) {
			List<String> batch = new LinkedList<String>();
			
			for( int j = 0; j < batchSize && wit.hasNext(); j++ ) {
				batch.add(wit.next());
			}
			
			super.notifyWorkers(subject, message, batch.toArray(new String[batch.size()]));
			i += batch.size();
						
			System.out.println(i + " workers notified, sleeping for a bit...");
			try { Thread.sleep(1000); } catch (InterruptedException e) {}
		}
		
		return true;
	}

	/**
	 * Disable all HITs found on MTurk. You should probably only use this with the sandbox.
	 * @return
	 */
	public int disableAllHITs() {
				
		int deleted = 0;		
								
		HIT[] hits = super.searchAllHITs();

		for( HIT hit : hits ) {
			this.safeDisableHIT(hit.getHITId());				
			deleted++;					
		}			
		
		logger.info("Disabled " + deleted + " hits");
		return deleted;
	}

	public int disableUnassignedHITs() {
		int deleted = 0;		

		HIT[] hits = searchAllHITs();
		
		logger.info("{} total HITs found", hits.length);

		for (HIT hit : hits ) {
			
			System.out.printf("HIT %s has status %s (Pending:%d, Available:%d, Completed:%d)\n",
					hit.getHITId(), hit.getHITStatus().toString(),
					hit.getNumberOfAssignmentsPending(), hit.getNumberOfAssignmentsAvailable(), 
					hit.getNumberOfAssignmentsCompleted());

			HITStatus hitStatus = hit.getHITStatus();

			// TODO we're assuming here that each HIT just has one assignment
			if( hit.getNumberOfAssignmentsAvailable() == 1) {					
				if( !HITStatus.Assignable.equals(hitStatus) ) {
					logger.info("Hit has 1 assignment but not assignable: " + hitStatus);
					continue;
				}
				logger.info("HIT " + hit + " is available, will be disabled");					
							
				safeDisableHIT(hit.getHITId());							
				deleted++;								
			}
			else if (HITStatus.Unassignable.equals(hitStatus) && hit.getNumberOfAssignmentsPending() == 1) {
				logger.info("HIT " + hit + " is being held, will be disabled");

				/*
				 * TODO this is a copy of above code, fix
				 * This will force someone to return the HIT, if they can't connect
				 */
				safeDisableHIT(hit.getHITId());							
				deleted++;
			}
		}
		
		logger.info("Disabled " + deleted + " hits");	
		return deleted;
	}

	/**
	 * Disable a hit with retries for throttling.
	 * @param hitId
	 */
	public void safeDisableHIT(String hitId) {		
		int tries = 0;
		do {
			try {
				super.disableHIT(hitId);
				logger.info("Disabled " + hitId);
				break;
			}
			catch( ObjectDoesNotExistException e ) {
				logger.warn("Could not disable HIT {} as it doesn't exist", hitId);
				e.printStackTrace();
				break;				
			} catch (ServiceException e) {
				e.printStackTrace();					
				if(++tries < MAX_RETRIES) throw e;
				
				logger.warn("Unexpected error; Throttling HIT deleting");			
				// Throttle it a bit
				try { Thread.sleep(THROTTLE_SLEEP_TIME); } 
				catch (InterruptedException e1) { }				
			}
		} while(true);		
	}
}
