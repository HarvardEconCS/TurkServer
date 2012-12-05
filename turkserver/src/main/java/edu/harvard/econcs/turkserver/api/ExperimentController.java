package edu.harvard.econcs.turkserver.api;

import edu.harvard.econcs.turkserver.server.MessageException;

public interface ExperimentController {

	String getExpId();

	/**
	 * Send a JSON-encoded message to the entire group.
	 * @param msg
	 */
	void sendExperimentBroadcast(Object msg) throws MessageException;	
	
	/**
	 * Set the experiment up to enable round tracking
	 */
	void startRounds();
	
	/**
	 * Get the current round of the experiment
	 * @return the current round
	 */
	int getCurrentRound();
	
	/**
	 * Finish the current round of the experiment
	 * Saves logs for the current logs
	 */
	void finishRound();
	
	/**
	 * Finish the experiment
	 */
	void finishExperiment();

	long getStartTime();
	
	long getFinishTime();
	
}
