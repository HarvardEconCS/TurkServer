package edu.harvard.econcs.turkserver.api;

public interface ClientLobbyController extends ClientController {

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
	 * Send username to the server
	 * @param username
	 */
	public void sendUsername(String username);
}
