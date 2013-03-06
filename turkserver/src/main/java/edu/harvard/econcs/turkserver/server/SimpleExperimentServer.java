/**
 * 
 */
package edu.harvard.econcs.turkserver.server;

import javax.swing.SwingUtilities;

import org.apache.commons.configuration.Configuration;
import org.cometd.bayeux.server.ServerSession;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.server.gui.ServerPanel;
import edu.harvard.econcs.turkserver.server.gui.TSTabbedPanel;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

/**
 * @author mao
 * 
 * A simple experiment server supporting one-way communication via JSON
 * with javascript clients.
 * 
 * Useful for experiments that don't require interaction between clients
 *
 */
@Singleton
public final class SimpleExperimentServer extends SessionServer {	
				
	final GUIListener guiListener;	
	
	@Inject
	public SimpleExperimentServer(			
			ExperimentDataTracker tracker, 
			HITController hitCont,
			WorkerAuthenticator workerAuth,
			Experiments experiments,
			JettyCometD jetty,
			Configuration config,
			final TSTabbedPanel guiTabs
			) throws Exception {
		
		super(tracker, hitCont, workerAuth, experiments, jetty, config);
		
		jetty.addServlet(SessionServlet.class, "/exp");
		
		final ServerPanel serverGUI = new ServerPanel(this, new Lobby.NullLobby());
		
		SwingUtilities.invokeLater(new Runnable() {	public void run() {
			guiTabs.addPanel("Server", serverGUI);				
		} });		
		
		this.guiListener = new GUIListener(this, serverGUI);
		experiments.registerListener(guiListener); 
	}	

	// TODO: refactor these somewhere better
	@Override
	public int getExpsInProgress() {
		return guiListener.inProgress.get();
	}

	@Override
	public int getExpsCompleted() {
		return guiListener.completed.get();
	}

	@Override
	protected HITWorkerImpl sessionAccept(ServerSession session, 
			String hitId, String assignmentId, String workerId) {
		
		/*
		 * At this point, the session is successfully authenticated, so we create an experiment  
		 */
		HITWorkerImpl hitw = super.sessionAccept(session, hitId, assignmentId, workerId);
		if( hitw == null ) return null;
		
		if( experiments.workerIsInProgress(hitw) ) {
			super.sessionReconnect(session, hitw);			
		}
		else {
			ExperimentControllerImpl exp = experiments.startSingle(hitw);	
		}				
		
		return hitw;
	}

	@Override
	protected void runServerInit() {
		logger.info("Simple server...nothing to do here");		
	}
	
	
}
