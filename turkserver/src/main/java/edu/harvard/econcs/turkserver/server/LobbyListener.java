package edu.harvard.econcs.turkserver.server;

interface LobbyListener {

	void broadcastLobbyMessage(Object data);
	
	void createNewExperiment(HITWorkerGroupImpl group);
	
	int getNumExperimentsRunning();
	
	int getNumUsersConnected();
}
