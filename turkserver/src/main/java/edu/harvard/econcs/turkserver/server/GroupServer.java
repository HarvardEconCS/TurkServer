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

import javax.swing.SwingUtilities;

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
	GUIListener guiListener;
	private ServerPanel serverGUI;
	
	@Inject
	public GroupServer(			
			ExperimentDataTracker tracker,
			HITController hitCont,
			WorkerAuthenticator workerAuth,
			Experiments experiments,			
			Configuration config,
			Lobby lobby			
			) throws ClassNotFoundException {		
		super(tracker, hitCont, workerAuth, experiments, config);
		
		logger.info("Debug mode set to {}", debugMode);
		
		this.requireUsernames = config.getBoolean(TSConfig.SERVER_USERNAME);
		this.lobby = lobby;
																		
		lobby.setListener(new ServerLobbyListener());				
	}
	
	@Inject(optional=true)
	public void injectWebServer(JettyCometD jetty) {
		super.injectWebServer(jetty);
		jetty.addServlet(GroupServlet.class, "/exp");
	}
	
	@Inject(optional=true) 
	public void injectGUI(final TSTabbedPanel guiTabs) {
		serverGUI = new ServerPanel(this, lobby);
		
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
	HITWorkerImpl sessionAccept(ServerSession conn,
			String hitId, String assignmentId, String workerId) {
		
		HITWorkerImpl hitw = super.sessionAccept(conn, hitId, assignmentId, workerId);
		if( hitw == null ) return null;				
		
		// Ask for username if we require it or somehow didn't get it last time		
		if( this.requireUsernames && hitw.getUsername() == null ) {
			Map<String, String> data = ImmutableMap.of(
					"status", Codec.status_usernameneeded
					);			
			
			SessionUtils.sendServiceMsg(conn, data);
		}		
		
		// Check if we should reconnect this HITWorker to an existing experiment
		boolean inExperiment;
		
		synchronized(lobby) { // Make sure starting experiments are atomic
			if( inExperiment = experiments.workerIsInProgress(hitw) ) {
				sessionReconnect(conn, hitw);
			}
			else {
				lobby.userJoined(hitw);
			}
		}
		
		if( !inExperiment ) {
			Map<String, String> data = ImmutableMap.of(
					"status", Codec.status_connectlobby					
					);
			SessionUtils.sendServiceMsg(conn, data);
			
			logger.info(hitw.toString() + " connected to lobby");
			
			if( serverGUI != null ) serverGUI.updateLobbyModel();			
		}
		
		return hitw;
	}

	/**
	 * 
	 * @param conn
	 * @param username
	 * @return true if this should be sent to the whole lobby
	 */
	public boolean lobbyLogin(ServerSession conn, String username) {
		// TODO: this is in the wrong place
		
		HITWorkerImpl hitw = clientToHITWorker.get(conn);
		
		if( hitw == null ) {
			logger.error("Can't accept username for unknown session {}", conn.getId());
			return false;
		}
		
		tracker.saveUsername(hitw, username);		
		return true;
	}

	public boolean lobbyUpdate(ServerSession conn, Map<String, Object> data) {
		HITWorkerImpl hitw = clientToHITWorker.get(conn);
		if( hitw == null ) {
			logger.error("Can't accept update status for unknown session {}", conn.getId());
			return false;
		}
		// NOTE: other way to fix lobby bug is to sync on the below, but not necessary since it just ignores message
		else if (experiments.workerIsInProgress(hitw)) {
			logger.info("Ignoring lobby update for {} in experiment", hitw);
			return false;
		}
		
		if( lobby.updateStatus(hitw, data) )
			serverGUI.updateLobbyModel();
		
		return true;
	}

	@Override
	boolean groupCompleted(HITWorkerGroup group) {
		boolean completed;
		
		if ( completed = super.groupCompleted(group) ) {
			/* 
			 * send a message to people that took HITs after the deadline
			 * and kick out still-connected clients
			 * 
			 * Only notify people in lobby, not (experiment people need to submit)		
			 */
			for( HITWorkerImpl worker : lobby.getLobbyUsers() )
				SessionUtils.sendStatus(worker.cometdSession.get(), 
						Codec.status_batchfinished, Messages.BATCH_COMPLETED);
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
	public void sessionDisconnect(ServerSession conn) {
		HITWorkerImpl worker = clientToHITWorker.get(conn);
		
		if( worker != null ) {
			if (lobby.userQuit(worker))	serverGUI.updateLobbyModel();
		}		
		
		// This takes care of disconnecting in the tracker
		super.sessionDisconnect(conn);
	}

}