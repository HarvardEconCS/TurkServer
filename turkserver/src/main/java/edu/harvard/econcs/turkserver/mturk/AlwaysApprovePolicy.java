package edu.harvard.econcs.turkserver.mturk;

import java.util.List;

import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.schema.Round;
import edu.harvard.econcs.turkserver.schema.Session;

/**
 * This always pays and puts the same feedback for both messages.
 * @author mao
 *
 */
public class AlwaysApprovePolicy implements PaymentPolicy {

	final String feedback;
	
	public AlwaysApprovePolicy(String feedback) {
		this.feedback = feedback;
	}	

	@Override
	public boolean processSession(Session session, Experiment experiment, List<Round> experimentRounds) {	
		return true;
	}

	@Override
	public String getLastAssignmentFeedback() {
		return feedback;
	}

	@Override
	public String getLastBonusFeedback() {		
		return feedback;
	}

}
