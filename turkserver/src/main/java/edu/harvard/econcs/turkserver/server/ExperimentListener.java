package edu.harvard.econcs.turkserver.server;

interface ExperimentListener {

	void experimentStarted(ExperimentControllerImpl exp);
	
	void roundStarted(ExperimentControllerImpl exp);
	
	void experimentFinished(ExperimentControllerImpl exp);
	
}
