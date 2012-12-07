package edu.harvard.econcs.turkserver.mturk;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.ClientConfig;
import com.google.inject.Inject;

/**
 * Class for connecting to MTurk and submitting REST requests.
 * @author mao
 *
 */
public class RequesterServiceExt extends RequesterService {
	
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

}
