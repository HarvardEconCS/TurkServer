package edu.harvard.econcs.turkserver.logging;

public interface LogController {

	void initialize(long startTime, String experimentId);

	void startRound(int round);
	
	/**
	 * Write the end-of-round log message and return the log for the round
	 * @return
	 */
	void finishRound();
	
	String getRoundOutput();

	long conclude();

	/**
	 * Get the log of the experiment, or the current round if there is one
	 * @return
	 */
	String getOutput();

}
