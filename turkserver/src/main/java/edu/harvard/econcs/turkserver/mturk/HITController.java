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
	 * Post a batch of hits starting with some amount, then over time
	 * @param initialAmount
	 * @param delay
	 * @param totalAmount
	 */
	void postBatchHITs(int initialAmount, int delay, int totalAmount);

	/**
	 * Called by the server to expire all remaining HITs once a set is finished
	 */
	void disableAndShutdown();
	
}
