package edu.harvard.econcs.turkserver.server;

import java.util.concurrent.atomic.AtomicInteger;

import edu.harvard.econcs.turkserver.server.gui.ServerPanel;

class GUIListener implements ExperimentListener {
	/**
	 * 
	 */
	private final SessionServer simpleExperimentServer;
	AtomicInteger inProgress = new AtomicInteger(0);
	AtomicInteger completed = new AtomicInteger(0);
	
	final ServerPanel serverGUI;
	
	GUIListener(SessionServer server, ServerPanel serverPanel) {
		this.simpleExperimentServer = server;
		this.serverGUI = serverPanel;
	}
	
	@Override
	public void experimentStarted(ExperimentControllerImpl exp) {			
		serverGUI.newExperiment(exp);	
		inProgress.incrementAndGet();
	}

	@Override
	public void roundStarted(ExperimentControllerImpl exp) {
		// No need to do anything as a timer updates exps
	}

	@Override
	public void experimentFinished(ExperimentControllerImpl exp) {			
		inProgress.decrementAndGet();
		this.simpleExperimentServer.groupCompleted(exp.group);
		serverGUI.finishedExperiment(exp);
		completed.incrementAndGet();
	}		
}