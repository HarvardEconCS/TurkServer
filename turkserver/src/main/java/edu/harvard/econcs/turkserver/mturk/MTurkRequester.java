package edu.harvard.econcs.turkserver.mturk;

import edu.harvard.econcs.turkserver.mturk.response.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Class for connecting to MTurk and submitting REST requests.
 * @author mao
 *
 */
public class MTurkRequester extends MTurkRequesterRaw {
	
	public MTurkRequester(String awsAccessKeyID, String awsSecretAccessKey,	boolean sandbox) {
		super(awsAccessKeyID, awsSecretAccessKey, sandbox);	
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
	public CreateHITResponse createHITExternalFromID(String hitTypeID, String url, int frameHeight, String lifetimeInSeconds)
	throws TurkException
	{
		String question = String.format(
				"<ExternalQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd\">" +
				"<ExternalURL>%s</ExternalURL>" +
				"<FrameHeight>%d</FrameHeight>" +
				"</ExternalQuestion>",
				// Need to escape XML entities going into this structure
				StringEscapeUtils.escapeXml(url), frameHeight);		

		return createHITFromID(hitTypeID, question, lifetimeInSeconds);
	}

	/**
	 * Sends a message to all workers in a list.
	 * 
	 * @param subject
	 * @param message
	 * @param workers
	 * @return
	 * @throws TurkException
	 */
	public boolean notifyAllWorkers(String subject, String message, 
			List<String> workers, int skipAmount) throws TurkException {
		Iterator<String> wit = workers.iterator();			
		
		int i = 0;
		while( wit.hasNext() && i < skipAmount ) {
			wit.next(); i++;
		}
		
		logger.info("Skipped " + i + " workers");
		
		while( wit.hasNext() ) {
			NotifyWorkersResponse nwr = super.notifyWorkers(subject, message, wit);
			i += 10;
			
			// TODO this kind of check is never used
			if( !nwr.isValid() ) return false;
			
			System.out.println(i + " workers notified, sleeping for a bit...");
			try { Thread.sleep(5000); } catch (InterruptedException e) {}
		}
		
		return true;
	}
	
	public String registerHITTypeWithQual(String title, String description, String keywords, 
			String rewardAmountInUSD, int assignmentDurationInSeconds, int autoApprovalDelayInSeconds,			
			List<Qualification> qualifications) throws TurkException {
		
		StringBuffer qualsb = new StringBuffer();				
		
		int i = 1; // first qual is approval rate
		for( Qualification qual : qualifications ) {			
			qual.appendQualString(qualsb, i);			
			i++;
		}
				
		RegisterHITTypeResponse resp = super.registerHITType(title, description, keywords, 
				rewardAmountInUSD, assignmentDurationInSeconds, autoApprovalDelayInSeconds, qualsb.toString());
		
		return resp.getHITTypeId();		
	}

}
