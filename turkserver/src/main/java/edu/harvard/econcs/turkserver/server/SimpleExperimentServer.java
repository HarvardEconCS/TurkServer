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
				
	GUIListener guiListener;	
	
	@Inject
	public SimpleExperimentServer(			
			ExperimentDataTracker tracker, 
			HITController hitCont,
			WorkerAuthenticator workerAuth,
			Experiments experiments,			
			Configuration config			
			) throws Exception {		
		super(tracker, hitCont, workerAuth, experiments, config);								
	}
	
	@Inject(optional=true)
	public void injectWebServer(JettyCometD jetty) {
		super.injectWebServer(jetty);
		jetty.addServlet(SessionServlet.class, "/exp");
	}
	
	@Inject(optional=true) 
	public void injectGUI(final TSTabbedPanel guiTabs) {
		// TODO: divide up a null lobby from a mock lobby
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
	protected HITWorkerImpl sessionAccept(ServerSession conn, 
			String hitId, String assignmentId, String workerId) {
		
		/*
		 * At this point, the session is successfully authenticated, so we create an experiment  
		 */
		HITWorkerImpl hitw = super.sessionAccept(conn, hitId, assignmentId, workerId);
		if( hitw == null ) return null;
		
		if( experiments.workerIsInProgress(hitw) ) {
			super.sessionReconnect(conn, hitw);			
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
