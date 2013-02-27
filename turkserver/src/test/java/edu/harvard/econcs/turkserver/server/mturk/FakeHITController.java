package edu.harvard.econcs.turkserver.server.mturk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.mturk.requester.QualificationRequirement;

import edu.harvard.econcs.turkserver.mturk.HITController;

public class FakeHITController implements HITController {

	final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
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
	public void postBatchHITs(int initialAmount, int delay, int totalAmount) {
		
		logger.info("Would post {} hits with delay {}", totalAmount, delay);

	}

	@Override
	public void disableAndShutdown() {
		// TODO Auto-generated method stub

	}

}
