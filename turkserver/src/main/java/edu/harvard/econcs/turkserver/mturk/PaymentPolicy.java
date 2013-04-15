package edu.harvard.econcs.turkserver.mturk;

import java.util.List;

import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.schema.Round;
import edu.harvard.econcs.turkserver.schema.Session;

public interface PaymentPolicy {

	/**
	 * Whether the base reward should be paid for a session
	 * @param session
	 * @return true if the base reward should be paid, otherwise rejected
	 */
	boolean shouldPayBaseReward(Session session);

	/**
	 * Get the feedback to go along with the last base reward decision
	 * @return
	 */
	String getLastAssignmentFeedback();

	/**
	 * Decide if the recorded bonus should be changed and adjust it
	 * @param session
	 * @param experiment
	 * @param experimentRounds
	 */
	void checkAndAdjustBonus(Session session, Experiment experiment, List<Round> experimentRounds);

	/**
	 * Get the feedback to go along with the last checked bonus payment
	 * @return
	 */
	String getLastBonusFeedback();
	
	

}
