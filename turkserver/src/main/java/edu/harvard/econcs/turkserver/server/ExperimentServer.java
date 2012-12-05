/**
 * 
 */
package edu.harvard.econcs.turkserver.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Inject;

import org.cometd.bayeux.server.LocalSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.harvard.econcs.turkserver.Codec;

import net.andrewmao.misc.Utils;


/**
 * @author Mao
 *
 */
@Deprecated
public abstract class ExperimentServer {	
		
	protected final HostServer host;	
	protected final LocalSession expBroadcaster;
	
	// Keeps track of whether clients are connected
	protected final ConcurrentHashMap<String, Boolean> clients;
	// Keeps track of the last time someone disconnected, if at all
	private final ConcurrentHashMap<String, Long> disconnectTime;	
	// Keeps track of the total number of milliseconds inactive
	private final ConcurrentHashMap<String, AtomicLong> inactiveTime;
	
	protected final long timeLimit;
	
	public final String experimentID;	
		
	public ExperimentServer(
			HostServer host, 
			ConcurrentHashMap<String, Boolean> clients, 
			long timeLimit) {				
		this.host = host;	
		this.clients = clients;
		this.timeLimit = timeLimit;
		
		// Set this so the experiment has an ID immediately
		experimentID = Utils.getCurrentTimeZoneTimeAsString();
		
		// Initialize inactivity tracking
		disconnectTime = new ConcurrentHashMap<String, Long>();
		inactiveTime = new ConcurrentHashMap<String, AtomicLong>();
		for( String id : clients.keySet() ) inactiveTime.put(id, new AtomicLong(0));
				
		expBroadcaster = host.bayeux.newLocalSession(getChannelName());
		logger.info("Exp Server local channel: " + getChannelName());
		expBroadcaster.handshake();
	}
	
	public Set<String> getClients() { return clients.keySet(); }		
	
	// Gets the name of the file associated with this experiment, if any
	public abstract String getFilename();

	public String toString() {
		return "Experiment " + experimentID; 
	}
	
	public String getChannelName() {
		return experimentID.replace(" ", "_");
	}

	public int size() {
		return clients.size();
	}	

	/**
	 * Returns the percent of time someone was inactive during the experiment,
	 * after it is finished
	 * @param id
	 * @return
	 */
	public double getInactivePercent(String id) {
		double iTime = inactiveTime.get(id).get();
		long totalTime = expFinishTime - expStartTime;
		return iTime / totalTime;
	}

	/**
	 * Adds milliseconds of inactive time to a client
	 * @param id
	 * @param time
	 * @return
	 */
	protected long addInactiveTime(String id, long time) {
		return inactiveTime.get(id).addAndGet(time);
	}
	
	/**
	 * Pulls all the disconnect times out of the table and adds the difference from finish time
	 * to the count of inactivity
	 */
	protected void finalizeInactiveTime() {
		Iterator<Map.Entry<String, Long>> it = disconnectTime.entrySet().iterator();
		
		while( it.hasNext() ) {
			Map.Entry<String, Long> e = it.next();			
			addInactiveTime(e.getKey(), expFinishTime - e.getValue());
			it.remove();
		}
	}
	
	/**
	 * Puts the client in a connected time and records the time since last disconnection, if any
	 * @param id
	 */
	public void clientConnected(String id) {
		// If reconnect, count the amount of time they were gone
		// TODO make sure this works with not allowing people to reconnect when done
		if( clients.containsKey(id) && (clients.get(id) == false)) {
			long discTime = disconnectTime.remove(id);
			addInactiveTime(id, System.currentTimeMillis() - discTime);
		}
		
		clients.put(id, true);
	}	

	/**
	 * Puts the client in a disconnected state and records the time
	 * @param id
	 */
	public void clientDisconnected(String id) {
		clients.put(id, false);
		
		// Record disconnect time
		disconnectTime.put(id, System.currentTimeMillis());
	}

}
