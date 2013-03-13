package edu.harvard.econcs.turkserver.mturk;

import com.amazonaws.mturk.requester.QualificationRequirement;

import edu.harvard.econcs.turkserver.mturk.HITController;

public class FakeHITController implements HITController {

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setHITType(String title, String description, String keywords,
			double reward, long assignmentDurationInSeconds,
			long autoApprovalDelayInSeconds,
			QualificationRequirement[] qualRequirements) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setExternalParams(String url, int frameHeight, int lifetime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void postBatchHITs(int target, int minOverhead, int maxOverhead, int minDelay, double pctOverhead) {		
		System.out.printf("Would post up to %d hit overhead with delay %d\n", target + maxOverhead, minDelay);
	}

	@Override
	public void disableHIT(String hitId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disableAndShutdown() {
		// TODO Auto-generated method stub

	}

}
