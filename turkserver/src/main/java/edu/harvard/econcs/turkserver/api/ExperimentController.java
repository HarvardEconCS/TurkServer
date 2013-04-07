package edu.harvard.econcs.turkserver.api;

import java.util.Map;

public interface ExperimentController {

	String getExpId();

	/**
	 * Send a JSON-encoded message to the entire group.
	 * @param msg
	 */
	void sendExperimentBroadcast(Map<String, Object> msg);	
	
	/**
	 * Sets the treatment/data associated with the current round. 
	 * @param inputData
	 */
	void setRoundInput(String inputData);

	/**
	 * Set the bonus that a worker will be paid upon completion.
	 * @param hitWorker
	 * @param amount
	 */
	void setBonusAmount(HITWorker hitWorker, double amount);

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

	/**
	 * Get the start time of the experiment
	 * @return
	 */
	long getStartTime();
	
	/**
	 * Get the finished time of the experiment
	 * @return
	 */
	long getFinishTime();
	
}
