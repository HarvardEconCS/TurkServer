package edu.harvard.econcs.turkserver.client;

import java.math.BigInteger;

public interface AbstractExpClient {

	public static enum State { DISCONNECTED, LOBBY, EXPERIMENT }

	/**
	 * Get the session ID, or none if not applicable.
	 * @return
	 */
	public BigInteger getSessionBigInt();
	
	/**
	 * Update the ready status
	 * @param isReady
	 */
	public void updateLobbyReadiness(boolean isReady);
	
	/**
	 * Update the lobby readiness of the client and the status message
	 * @param isReady
	 */
	public void updateLobbyStatus(String statusMsg);
	
	/**
	 * Send an update to the server with some amount of time inactive
	 * @param timeInactive
	 */
	public void recordInactivity(long timeInactive);
	
}