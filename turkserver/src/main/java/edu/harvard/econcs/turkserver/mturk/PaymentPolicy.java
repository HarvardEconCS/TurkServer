package edu.harvard.econcs.turkserver.mturk;

import java.util.List;

import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.schema.Round;
import edu.harvard.econcs.turkserver.schema.Session;

public interface PaymentPolicy {

	/**
	 * Whether the base reward should be paid for a session
	 * Adjust the recorded bonus as necessary
	 * 
	 * @param session
	 * @param experiment
	 * @param experimentRounds
	 * 
	 * @return true if the base reward should be paid, otherwise rejected	 
	 */
	boolean processSession(Session session, Experiment experiment, List<Round> experimentRounds);

	/**
	 * Get the feedback to go along with the last base reward decision
	 * @return
	 */
	String getLastAssignmentFeedback();

	/**
	 * Get the feedback to go along with the last checked bonus payment
	 * @return
	 */
	String getLastBonusFeedback();
		
}
