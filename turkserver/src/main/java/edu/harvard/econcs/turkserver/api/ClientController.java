package edu.harvard.econcs.turkserver.api;

import edu.harvard.econcs.turkserver.QuizResults;

public interface ClientController {

	public String getHITId();
	
	public String getAssignmentId();
	
	public String getWorkerId();
	
	public String getUsername();

	public boolean isConnected();		
	
	public void sendQuizResults(QuizResults qr);
	
	public void sendExperimentBroadcast(Object data);

	public void sendExperimentService(Object data);

	/**
	 * Send an update to the server with some amount of time inactive
	 * @param timeInactive
	 */
	public void recordInactivity(long timeInactive);
	
}
