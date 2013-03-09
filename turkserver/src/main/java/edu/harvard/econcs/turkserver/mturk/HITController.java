package edu.harvard.econcs.turkserver.mturk;

import com.amazonaws.mturk.requester.QualificationRequirement;

/**
 * Controls methods for posting HITs on MTurk
 * and appropriately spamming to stay at the top of the list
 * 
 * @author mao
 *
 */
public interface HITController extends Runnable {

	void setHITType(
				String title, 
				String description,
				String keywords,
				double reward,
				long assignmentDurationInSeconds,
				long autoApprovalDelayInSeconds,			
				QualificationRequirement[] qualRequirements
				);

	void setExternalParams(String url, int frameHeight, int lifetime);
	
	/**
	 * Post a batch of hits, adaptively keeping some overhead above accepted HITs
	 * @param minOverhead
	 * @param maxOverhead
	 * @param minDelay
	 * @param pctOverhead
	 */
	void postBatchHITs(int target, int minOverhead, int maxOverhead, int minDelay, double pctOverhead);

	/**
	 * Called by the server to expire all remaining HITs once a set is finished
	 */
	void disableAndShutdown();
	
}
