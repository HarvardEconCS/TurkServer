/**
 * The main server that hosts experiments
 */
package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.*;
import edu.harvard.econcs.turkserver.api.*;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.server.gui.ServerPanel;
import edu.harvard.econcs.turkserver.server.gui.TSTabbedPanel;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerSession;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Mao
 *
 */
@Singleton
public final class GroupServer extends SessionServer {
	
	final boolean requireUsernames;	
		
	final Lobby lobby;
	private volatile LocalSession lobbyBroadcaster;

	// GUI
	final GUIListener guiListener;
	private final ServerPanel serverGUI;
	
	@Inject
	public GroupServer(			
			ExperimentDataTracker tracker,
			HITController hitCont,
			WorkerAuthenticator workerAuth,
			Experiments experiments,
			JettyCometD jetty,
			Configuration config,
			Lobby lobby,
			TSTabbedPanel guiTabs
			) throws ClassNotFoundException {		
		super(tracker, hitCont, workerAuth, experiments, jetty, config);
		
		logger.info("Debug mode set to {}", debugMode);
		
		this.requireUsernames = config.getBoolean(TSConfig.SERVER_USERNAME);
		this.lobby = lobby;
						
		jetty.addServlet(GroupServlet.class, "/exp");						
		
		guiTabs.addPanel("Server", serverGUI = new ServerPanel(this, lobby));
		
		this.guiListener = new GUIListener();
		experiments.registerListener(guiListener);
		
		lobby.setListener(new ServerLobbyListener());				
	}
	
	class GUIListener implements ExperimentListener {
		AtomicInteger inProgress = new AtomicInteger(0);
		AtomicInteger completed = new AtomicInteger(0);
		
		@Override
		public void experimentStarted(ExperimentControllerImpl exp) {			
			serverGUI.newExperiment(exp);	
			inProgress.incrementAndGet();
		}
	
		@Override
		public void roundStarted(ExperimentControllerImpl exp) {
			// TODO Auto-generated method stub
		}
	
		@Override
		public void experimentFinished(ExperimentControllerImpl exp) {			
			inProgress.decrementAndGet();
			groupCompleted(exp.group);
			serverGUI.finishedExperiment(exp);
			completed.incrementAndGet();
		}		
	}

	// TODO: refactor these somewhere better
	public int getExpsInProgress() {
		return guiListener.inProgress.get();
	}

	public int getExpsCompleted() {
		return guiListener.completed.get();
	}

	class ServerLobbyListener implements LobbyListener {
		@Override
		public void broadcastLobbyMessage(Object data) {		
			if( lobbyBroadcaster != null )
				bayeux.getChannel("/lobby").publish(lobbyBroadcaster, data, null);
			else
				logger.warn("Tried to send message but lobby wasn't ready yet: " + data.toString());
		}
	
		@Override
		public void createNewExperiment(HITWorkerGroupImpl expClients) {				
			ExperimentControllerImpl exp = experiments.startGroup(expClients);			
			serverGUI.updateLobbyModel();
		}
	
		@Override
		public int getNumExperimentsRunning() {		
			return guiListener.completed.get();
		}
	
		@Override
		public int getNumUsersConnected() {		
			return bayeux.getSessions().size();
		}
	}

	@Override
	HITWorkerImpl sessionAccept(ServerSession session,
			String hitId, String assignmentId, String workerId) {
		
		HITWorkerImpl hitw = super.sessionAccept(session, hitId, assignmentId, workerId);
		if( hitw == null ) return null;				
		
		// Ask for username if we require it or somehow didn't get it last time		
		if( this.requireUsernames && hitw.getUsername() == null ) {
			Map<String, String> data = ImmutableMap.of(
					"status", Codec.status_usernameneeded
					);			
			
			SessionUtils.sendServiceMsg(session, data);
		}		
		
		// Check if we should reconnect this HITWorker to an existing experiment
		boolean inExperiment;
		
		synchronized(lobby) {
			inExperiment = experiments.workerIsInProgress(hitw);
			
			if( inExperiment ) {									
				sessionReconnect(session, hitw);			
			} 
			else {
				Map<String, String> data = ImmutableMap.of(
						"status", Codec.status_connectlobby					
						);
				SessionUtils.sendServiceMsg(session, data);
				
				logger.info(hitw.toString() + " connected to lobby");
				lobby.userJoined(hitw);				
			}
		}
		
		if( !inExperiment ) serverGUI.updateLobbyModel();		
		
		return hitw;
	}

	@Override
	void sessionReconnect(ServerSession session, HITWorkerImpl hitw) {
		Map<String, String> data = ImmutableMap.of(
				"status", Codec.status_connectexp,
				"channel", hitw.expCont.expChannel
				);

		SessionUtils.sendServiceMsg(session, data);							

		super.sessionReconnect(session, hitw);
	}

	/**
	 * 
	 * @param session
	 * @param username
	 * @return true if this should be sent to the whole lobby
	 */
	public boolean lobbyLogin(ServerSession session, String username) {
		// TODO: this is in the wrong place
		
		HITWorkerImpl hitw = clientToHITWorker.get(session);
		
		if( hitw == null ) {
			logger.error("Can't accept username for unknown session {}", session.getId());
			return false;
		}
		
		tracker.saveUsername(hitw, username);		
		return true;
	}

	public boolean lobbyUpdate(ServerSession session, Map<String, Object> data) {
		HITWorkerImpl hitw = clientToHITWorker.get(session);
		if( hitw == null ) {
			logger.error("Can't accept update status for unknown session {}", session.getId());
			return false;
		}
		// NOTE: other way to fix lobby bug is to sync on the below, but not necessary since it just ignores message
		else if (experiments.workerIsInProgress(hitw)) {
			logger.info("Ignoring lobby update for {} in experiment", hitw);
			return false;
		}
		
		lobby.updateStatus(hitw, data);
		serverGUI.updateLobbyModel();
		
		return true;
	}

	@Override
	boolean groupCompleted(HITWorkerGroup group) {
		boolean completed;
		
		if ( completed = super.groupCompleted(group) ) {
			// Only notify people in lobby, not (experiment people need to submit)
			for( HITWorkerImpl worker : lobby.getLobbyUsers() )
				SessionUtils.sendStatus(worker.cometdSession.get(), Codec.status_batchfinished);
		}
		
		return completed;
	}

	@Override
	protected void runServerInit() {		
		logger.info("Creating lobby session");
		
		lobbyBroadcaster = bayeux.newLocalSession("lobby");
		lobbyBroadcaster.handshake();
				
	}
	
	@Override
	public void sessionDisconnect(ServerSession clientId) {
		HITWorkerImpl worker = clientToHITWorker.get(clientId);
		
		if( worker != null ) {
			if (lobby.userQuit(worker))	serverGUI.updateLobbyModel();
		}		
		
		// This takes care of disconnecting in the tracker
		super.sessionDisconnect(clientId);
	}

}
