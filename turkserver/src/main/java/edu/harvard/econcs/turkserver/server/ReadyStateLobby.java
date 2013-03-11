package edu.harvard.econcs.turkserver.server;

import java.util.LinkedList;
import java.util.List;
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
import com.google.inject.name.Named;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.config.TSConfig;

/**
 * Default lobby implementation where users that are ready are grouped into 
 * @author mao
 *
 */
public class ReadyStateLobby implements Lobby {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

	final boolean defaultStatus;
	final Configurator configurator;	
	
	private final ConcurrentBooleanCounter<HITWorkerImpl> lobbyStatus;		
	final AtomicReference<String> serverMessage;
	
	private LobbyListener lobbyListener;
	
	@Inject
	public ReadyStateLobby(
			@Named(TSConfig.EXP_CONFIGURATOR) Configurator configurator,
			Configuration conf
			) {
		
		this.configurator = configurator;
		this.defaultStatus = conf.getBoolean(TSConfig.SERVER_LOBBY_DEFAULT);
		
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
	public void userJoined(HITWorkerImpl hitw) {
		/*
		 * NOTE: Seemed possible to remove synchronization from this method
		 * Giving it a shot with lobby unit tests
		 */
		
		// Put default lobby status in here
		lobbyStatus.put(hitw, defaultStatus);
		
		if( defaultStatus ) {
			// Create debug experiments if we have enough players, default to ready			
			logger.info("lobby has {} people, {} ready", lobbyStatus.size(), lobbyStatus.getTrueCount());			
			tryExperimentStart();			
		}

		// TODO remove this and let users send their own ready message
		broadcastLobbyStatus();
	}
	
	private synchronized boolean tryExperimentStart() {
		// Don't try anything if not enough people in lobby
		int expSize = configurator.groupSize(); 
		if( lobbyStatus.getTrueCount() < expSize ) return false;
		
		// Generate the list of experiment clients					
		
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
				
//		System.out.println("Current lobby before sending: " + lobbyStatus);
//		System.out.println("Lobby sending out a group: " + expClients);
		
		lobbyListener.createNewExperiment(expClients);
		
		/*
		 * NOTE: They must be in experiment first before we can safely remove from lobby
		 * 
		 * Avoids race condition where workers removed from lobby but not yet in experiment
		 * may get placed in the lobby a second time, causing big problems
		 */
		for( HITWorker id : expClients.getHITWorkers() )	
			lobbyStatus.remove((HITWorkerImpl) id);
		
//		System.out.println("Current lobby after sending: " + lobbyStatus);
		return true;
	}

	@Override
	public boolean updateStatus(HITWorkerImpl hitw, Map<String, Object> data) {
		
		boolean isReady = defaultStatus || Boolean.parseBoolean(data.get("ready").toString());
		
		/*
		 * Ignore lobby updates for people not in lobby
		 * MONUMENT FOR MASSIVE DOUBLE-JOIN MYSTERY BUG
		 */
		Boolean oldStatus = lobbyStatus.replace(hitw, isReady);
		
		/*
		 * Do nothing if user was already removed from lobby
		 * or there was no change to the status
		 */
		if( oldStatus == null || oldStatus == isReady ) return false;
		
		tryExperimentStart();
		
		if( !defaultStatus && lobbyStatus.size() < configurator.groupSize() ) {				
			// Make sure everyone's ready is disabled
			for( HITWorkerImpl id : lobbyStatus.keySet() ) {
				lobbyStatus.replace(id, false);
			}
		}
		
		// Notify everyone who is remaining in the lobby
		broadcastLobbyStatus();
		
		return true;
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
		
		// TODO forward this quit message to lobby instead of a whole list
//		ServerSession session = worker.cometdSession.get();		 
//		Map<String, Object> data = new TreeMap<String, Object>();
//		data.put("status", "quit");
//		if( session != null ) data.put("id", session.getId());
//		data.put("username", worker.getUsername());
//		lobbyListener.broadcastLobbyMessage(data);
		
		broadcastLobbyStatus();
		
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
		int usersNeeded = configurator.groupSize();
		
		data.put("status", "update");
		
		data.put("numusers", usersInLobby);
		data.put("numneeded", usersNeeded);
		data.put("joinenabled", usersInLobby >= usersNeeded);
		
		data.put("servermsg", serverMessage.get());
		data.put("currentexps", lobbyListener.getNumExperimentsRunning());
		data.put("totalusers", lobbyListener.getNumUsersConnected());
		
		/* TODO could be some race conditions here if lobby size changes?
		 * i.e. ArrayIndexOutOfBounds when array size changes 
		 */
		List<Object> users = new LinkedList<Object>();		
		
		for( Map.Entry<HITWorkerImpl, Boolean> e : lobbyStatus.entrySet() ) {
			HITWorkerImpl user = e.getKey();
			ServerSession session = user.cometdSession.get();
			if( session == null ) continue;
			
			// clientId, username, and status
			users.add(new Object[] { session.getId(), user.getUsername(), e.getValue() });
		}
		data.put("users", users);
		
		lobbyListener.broadcastLobbyMessage(data);		
	}

}
