/**
 * The main server that hosts experiments
 */
package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.*;
import edu.harvard.econcs.turkserver.api.*;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.configuration.Configuration;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerSession;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.andrewmao.misc.ConcurrentBooleanCounter;

/**
 * @author Mao
 *
 */
@Singleton
public final class GroupServer extends SessionServer {
	
	final boolean requireUsernames;
	final boolean debugMode;
	final boolean lobbyEnabled;
	
	// Turk crap
	
	// Experiment and user information					
	final ConcurrentBooleanCounter<HITWorkerImpl> lobbyStatus;	
	
	final AtomicReference<String> serverMessage;	
	
	private LocalSession lobbyBroadcaster;

	// GUI
	private ServerFrame serverGUI;
	
	@Inject
	public GroupServer(			
			ExperimentDataTracker tracker,
			HITController hitCont,
			WorkerAuthenticator workerAuth,
			Experiments experiments,
			JettyCometD jetty,
			Configuration config
			) throws ClassNotFoundException {
		
		super(tracker, hitCont, workerAuth, experiments, jetty, config);
		
		this.requireUsernames = config.getBoolean(TSConfig.SERVER_USERNAME);
		this.debugMode = config.getBoolean(TSConfig.SERVER_DEBUGMODE);
		this.lobbyEnabled = config.getBoolean(TSConfig.SERVER_LOBBY);
		
		jetty.addServlet(GroupServlet.class, "/exp");						
				
		lobbyStatus = new ConcurrentBooleanCounter<HITWorkerImpl>();				
				
		serverMessage = new AtomicReference<String>("");
		
		serverGUI = new ServerFrame(this);
				
	}
	
	@Override
	protected HITWorkerImpl sessionAccept(ServerSession session,
			String hitId, String assignmentId, String workerId) {
		
		HITWorkerImpl hitw = super.sessionAccept(session, hitId, assignmentId, workerId);
		if( hitw == null ) return null;
		
		// Ask for username if we require it or somehow didn't get it last time		
		if( this.requireUsernames && hitw.getUsername() == null ) {
			Map<String, String> data = ImmutableMap.of(
					"status", Codec.usernameNeeded
					);			
			
			SessionUtils.sendServiceMsg(session, data);
		}		
		
		// Check if we should reconnect this HITWorker to an existing experiment
		if( experiments.workerIsInProgress(hitw) ) {
									
			sessionReconnect(session, hitw);
			
		} 
		else if( debugMode ) {
			// single-person debug experiments, so create a new one and skip lobby
			HITWorkerGroupImpl single = new HITWorkerGroupImpl();
			single.add(hitw);			
			
			logger.info("Creating new experiment in debug mode");
			createNewExperiment(single);
			serverGUI.updateLobbyModel();
		}
		else {
			Map<String, String> data = ImmutableMap.of(
					"status", Codec.connectLobbyAck					
					);
			
			SessionUtils.sendServiceMsg(session, data);
			
			logger.info(String.format("%s (%s) connected to lobby",	
					hitId, hitw.getUsername()));
		}
		
		return hitw;
	}

	@Override
	void sessionReconnect(ServerSession session, HITWorkerImpl hitw) {
		Map<String, String> data = ImmutableMap.of(
				"status", Codec.connectExpAck,
				"channel", Codec.expChanPrefix + hitw.expCont.expChannel
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
		HITWorkerImpl hitw = clientToHITWorker.get(session);
		
		if( hitw == null ) {
			logger.error("Can't accept username for unknown session {}", session.getId());
			return false;
		}
		
		tracker.saveUsername(hitw, username);		
		return true;
	}

	public boolean lobbyUpdate(ServerSession session, boolean isReady) {
		HITWorkerImpl hitw = clientToHITWorker.get(session);
		if( hitw == null ) {
			logger.error("Can't accept update status for unknown session {}", session.getId());
			return false;
		}
		
		lobbyStatus.put(hitw, isReady);				
		
		int neededPeople = experiments.getMinGroupSize();
		
		// are there enough people ready to start?
		synchronized(lobbyStatus) {	
			logger.info("Lobby has " + lobbyStatus.getTrueCount() + " ready people");				
			if( lobbyStatus.getTrueCount() >= neededPeople ) {				
				// Create a new experiment and assign the ready people to it
				createNewExperiment(null);				
			}
			else if( lobbyStatus.size() < neededPeople ) {				
				// Make sure everyone's ready is disabled
				for( HITWorkerImpl id : lobbyStatus.keySet() ) {
					lobbyStatus.put(id, false);
				}
			}
		}
							
		serverGUI.updateLobbyModel();
		
		// Notify everyone who is remaining in the lobby
		sendLobbyStatus();
		
		return true;
	}

	void sendLobbyStatus() {
		Map<String, Object> data = new TreeMap<String, Object>();
		
		int usersInLobby = lobbyStatus.size();
		int usersNeeded = experiments.getMinGroupSize();
		
		data.put("status", "update");
		
		data.put("numusers", usersInLobby);
		data.put("numneeded", usersNeeded);
		data.put("joinenabled", usersInLobby >= usersNeeded);
		
		data.put("servermsg", serverMessage.get());
		data.put("currentexps", experiments.getNumInProgress());
		data.put("totalusers", bayeux.getSessions().size());
		
		// TODO could be some race conditions here if lobby size changes?
		Object[] users = new Object[lobbyStatus.size()];		
		int i = 0;
		for( Map.Entry<HITWorkerImpl, Boolean> e : lobbyStatus.entrySet() ) {
			HITWorkerImpl user = e.getKey();
			ServerSession session = user.cometdSession.get();
			if( session == null ) continue;
			
			// clientId, username, and status
			users[i++]= new Object[] { session.getId(),	user.getUsername(), e.getValue() };
		}
		data.put("users", users);
		
		// TODO broadcast to lobby?
		bayeux.getChannel("lobby").publish(this.lobbyBroadcaster, data, null);
		
	}

	private void createNewExperiment(HITWorkerGroupImpl expClients) {
		
		// Generate the list of experiment clients
		if( expClients == null ) {			
			int expSize = experiments.getMinGroupSize();
			expClients = new HITWorkerGroupImpl();

			// Count up exactly expSize people for the new experiment
			int counter = 0;
			for( Map.Entry<HITWorkerImpl, Boolean> e : lobbyStatus.entrySet() ) {						
				if( e.getValue() == true ) {							
					expClients.add(e.getKey());
					counter++;
				}

				// Don't put more than the required number of people, even if more are ready
				if(counter == expSize) break;
			}
		}		
		
		ExperimentControllerImpl exp = experiments.startGroup(expClients);
		
		serverGUI.newExperiment(exp);	
		
		/* No problem in starting the exp - now can remove from lobby
		 * everyone in the new exp is removed before lobby is notified 
		 * due to synchronize over lobbyStatus above
		 * 
		 * Moved this down here to try and avoid race condition? 
		 * So that they are in experiment before being out of lobby - limbo state 
		 * since experiment is checked first
		 */			
		for( HITWorker id : expClients.getHITWorkers()) 
			lobbyStatus.remove((HITWorkerImpl) id);
	}
		
	@Override
	boolean groupCompleted(HITWorkerGroup group) {
		boolean completed;
		
		if ( completed = super.groupCompleted(group) ) {
			// Only notify people in lobby, not (experiment people need to submit)
			for( HITWorkerImpl worker : lobbyStatus.keySet() )
				SessionUtils.sendStatus(worker.cometdSession.get(), Codec.batchFinishedMsg);
		}
		
		return completed;
	}

	@Override
	protected void runServerInit() {		
		lobbyBroadcaster = bayeux.newLocalSession("lobby");
		lobbyBroadcaster.handshake();				
	}
	
	@Override
	public void sessionDisconnect(ServerSession clientId) {
		HITWorkerImpl worker = clientToHITWorker.get(clientId);
		
		// Was this dude in the lobby? If so remove him from the lobby and notify lobby ppl		
		if( worker != null && lobbyStatus.remove(worker) != null ) {			
			logger.info(String.format("%s (%s) removed from lobby",
					worker.getHitId(), worker.getUsername()));			
			ServerSession session = worker.cometdSession.get();
			
			// TODO check on this quit message to lobby, be more robust 
			
			Map<String, Object> data = new TreeMap<String, Object>();
			
			data.put("status", "quit");
			if( session != null ) data.put("id", session.getId());
			data.put("username", worker.getUsername());
			
			bayeux.getChannel("/lobby").publish(clientId, data, null);
		}
		
		// This takes care of disconnecting in the tracker
		super.sessionDisconnect(clientId);
	}

}
