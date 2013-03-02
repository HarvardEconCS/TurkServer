package edu.harvard.econcs.turkserver.api;

import java.util.Map;

import edu.harvard.econcs.turkserver.client.QuizResults;

public interface ClientController {

	public String getHitId();
	
	public String getAssignmentId();
	
	public String getWorkerId();
	
	public String getUsername();

	public boolean isConnected();		
	
	/**
	 * Order the client to disconnect from the server
	 */
	public void disconnect();
	
	public void sendQuizResults(QuizResults qr);
	
	/**
	 * Client method to send a broadcast message to the server
	 * @param data
	 */
	public void sendExperimentBroadcast(Map<String, Object> data);

	/**
	 * Client method to send a private message to the server
	 * @param data
	 */
	public void sendExperimentService(Map<String, Object> data);

	/**
	 * Send an update to the server with some amount of time inactive
	 * @param timeInactive
	 */
	public void recordInactivity(long timeInactive);
	
}
