package edu.harvard.econcs.turkserver.server;

public interface LobbyListener {

	/**
	 * Send a message to the lobby
	 * @param data
	 */
	void broadcastLobbyMessage(Object data);
	
	/**
	 * Callback to create an experiment with a particular set of users
	 */
	void createNewExperiment(HITWorkerGroupImpl group);
	
	int getNumExperimentsRunning();
	
	int getNumUsersConnected();
}
