package edu.harvard.econcs.turkserver.mturk;

import java.util.List;

import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.schema.Round;
import edu.harvard.econcs.turkserver.schema.Session;

public class AlwaysApprovePolicy implements PaymentPolicy {

	final String feedback;
	
	public AlwaysApprovePolicy(String feedback) {
		this.feedback = feedback;
	}
	
	@Override
	public boolean shouldPayBaseReward(Session session) {		
		return true;
	}

	@Override
	public String getLastAssignmentFeedback() {
		return feedback;
	}

	@Override
	public void checkAndAdjustBonus(Session session, Experiment experiment,
			List<Round> experimentRounds) {	
	}

	@Override
	public String getLastBonusFeedback() {		
		return feedback;
	}

}
