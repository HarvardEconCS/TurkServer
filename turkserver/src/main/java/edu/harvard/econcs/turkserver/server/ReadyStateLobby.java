package edu.harvard.econcs.turkserver.server;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.configuration.Configuration;
import org.cometd.bayeux.server.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.andrewmao.misc.ConcurrentBooleanCounter;

import com.google.inject.Inject;

import edu.harvard.econcs.turkserver.api.HITWorker;

/**
 * Default lobby implementation where users that are ready are grouped into 
 * @author mao
 *
 */
public class ReadyStateLobby implements Lobby {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

	final boolean debugMode;
	final Experiments experiments;	
	
	private final ConcurrentBooleanCounter<HITWorkerImpl> lobbyStatus;		
	final AtomicReference<String> serverMessage;
	
	private LobbyListener lobbyListener;
	
	@Inject
	public ReadyStateLobby(
			Experiments experiments,
			Configuration conf
			) {
		
		this.experiments = experiments;
		this.debugMode = conf.getBoolean(TSConfig.SERVER_DEBUGMODE);
		
		lobbyStatus = new ConcurrentBooleanCounter<HITWorkerImpl>();								
		serverMessage = new AtomicReference<String>("");
		
	}

	@Override
	public void setListener(LobbyListener listener) {
		lobbyListener = listener;
	}

	@Override
	public void setMessage(String text) {
		serverMessage.set(text);
		
		/* publish the message to lobby
		 * It's fine to broadcast here as it is a rare occurrence
		 */
		broadcastLobbyStatus();		
	}

	@Override
	public synchronized void userJoined(HITWorkerImpl hitw) {
		if( debugMode ) {
			// Create debug experiments if we have enough players, default to ready
			lobbyStatus.put(hitw, true);			
			logger.info("Debug mode: lobby has {} people, {} ready", lobbyStatus.size(), lobbyStatus.getTrueCount());
			
			tryExperimentStart();			
		}
		else {
			lobbyStatus.put(hitw, false);
		}		
	}
	
	private synchronized void tryExperimentStart() {
		// whether to start experiments
		if( lobbyStatus.getTrueCount() < experiments.getMinGroupSize() ) return;
		
		// Generate the list of experiment clients
					
		int expSize = experiments.getMinGroupSize();
		HITWorkerGroupImpl expClients = new HITWorkerGroupImpl();

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
				
		lobbyListener.createNewExperiment(expClients);
		
		/*
		 * Avoid race condition where workers removed from lobby but not yet in experiment
		 * may get placed in the lobby a second time, causing big problems
		 */
		for( HITWorker id : expClients.getHITWorkers() )	
			lobbyStatus.remove((HITWorkerImpl) id);
	}

	@Override
	public void updateStatus(HITWorkerImpl hitw, Map<String, Object> data) {
		boolean isReady = Boolean.parseBoolean(data.get("ready").toString());
						
		// are there enough people ready to start?
		synchronized(this) {
			lobbyStatus.put(hitw, debugMode || isReady);				
			int neededPeople = experiments.getMinGroupSize();
			
			logger.info("Lobby has " + lobbyStatus.getTrueCount() + " ready people");				
			if( lobbyStatus.getTrueCount() >= neededPeople ) {				
				// Create a new experiment and assign the ready people to it
				tryExperimentStart();				
			}
			else if( !debugMode && lobbyStatus.size() < neededPeople ) {				
				// Make sure everyone's ready is disabled
				for( HITWorkerImpl id : lobbyStatus.keySet() ) {
					lobbyStatus.put(id, false);
				}
			}
		}
		
		// Notify everyone who is remaining in the lobby
		broadcastLobbyStatus();
	}

	@Override
	public boolean userQuit(HITWorkerImpl worker) {		
		// Was this dude in the lobby? If so remove him from the lobby and notify lobby ppl
		if( worker == null ) return false;
		
		synchronized(this) {
			 if( lobbyStatus.remove(worker) == null ) return false;
		}
		
		logger.info(String.format("%s (%s) removed from lobby",
				worker.getHitId(), worker.getUsername()));			
		ServerSession session = worker.cometdSession.get();

		// TODO check on this quit message to lobby, be more robust 

		Map<String, Object> data = new TreeMap<String, Object>();

		data.put("status", "quit");
		if( session != null ) data.put("id", session.getId());
		data.put("username", worker.getUsername());

		lobbyListener.broadcastLobbyMessage(data);
		
		return true;
	}

	@Override
	public Set<HITWorkerImpl> getLobbyUsers() {		
		return lobbyStatus.keySet();
	}
	
	@Override
	public Object getStatus(HITWorkerImpl hitw) {
		return lobbyStatus.get(hitw);
	}

	void broadcastLobbyStatus() {
		Map<String, Object> data = new TreeMap<String, Object>();
		
		int usersInLobby = lobbyStatus.size();
		int usersNeeded = experiments.getMinGroupSize();
		
		data.put("status", "update");
		
		data.put("numusers", usersInLobby);
		data.put("numneeded", usersNeeded);
		data.put("joinenabled", usersInLobby >= usersNeeded);
		
		data.put("servermsg", serverMessage.get());
		data.put("currentexps", lobbyListener.getNumExperimentsRunning());
		data.put("totalusers", lobbyListener.getNumUsersConnected());
		
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
		
		lobbyListener.broadcastLobbyMessage(data);
		
	}

}
