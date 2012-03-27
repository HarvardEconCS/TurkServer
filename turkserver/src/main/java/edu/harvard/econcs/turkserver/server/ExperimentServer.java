/**
 * 
 */
package edu.harvard.econcs.turkserver.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.cometd.bayeux.server.LocalSession;

import edu.harvard.econcs.turkserver.Codec;

import net.andrewmao.misc.Utils;


/**
 * @author Mao
 *
 */
public abstract class ExperimentServer<T extends ExperimentServer<T>> implements Runnable {
	
	protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
		
	protected final HostServer<T> host;	
	protected final LocalSession expBroadcaster;
	
	// Keeps track of whether clients are connected
	protected final ConcurrentHashMap<BigInteger, Boolean> clients;
	// Keeps track of the last time someone disconnected, if at all
	private final ConcurrentHashMap<BigInteger, Long> disconnectTime;	
	// Keeps track of the total number of milliseconds inactive
	private final ConcurrentHashMap<BigInteger, AtomicLong> inactiveTime;
	
	protected final long timeLimit;
	
	protected volatile long expStartTime;
	protected volatile long expFinishTime;
	
	public final String experimentID;	
	
	private PrintWriter experimentLog = null;
	
	public ExperimentServer(HostServer<T> host, ConcurrentHashMap<BigInteger, Boolean> clients, long timeLimit) {				
		this.host = host;	
		this.clients = clients;
		this.timeLimit = timeLimit;
		
		// Set this so the experiment has an ID immediately
		experimentID = Utils.getCurrentTimeZoneTimeAsString();
		
		// Initialize inactivity tracking
		disconnectTime = new ConcurrentHashMap<BigInteger, Long>();
		inactiveTime = new ConcurrentHashMap<BigInteger, AtomicLong>();
		for( BigInteger id : clients.keySet() ) inactiveTime.put(id, new AtomicLong(0));
				
		expBroadcaster = host.bayeux.newLocalSession(getChannelName());
	}
	
	public Set<BigInteger> getClients() { return clients.keySet(); }		
	
	// Gets the name of the file associated with this experiment, if any
	public abstract String getFilename();

	public String toString() {
		return "Experiment " + experimentID; 
	}
	
	public String getChannelName() {
		return experimentID.replace(" ", "");
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
	public double getInactivePercent(BigInteger id) {
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
	protected long addInactiveTime(BigInteger id, long time) {
		return inactiveTime.get(id).addAndGet(time);
	}
	
	/**
	 * Pulls all the disconnect times out of the table and adds the difference from finish time
	 * to the count of inactivity
	 */
	protected void finalizeInactiveTime() {
		Iterator<Map.Entry<BigInteger, Long>> it = disconnectTime.entrySet().iterator();
		
		while( it.hasNext() ) {
			Map.Entry<BigInteger, Long> e = it.next();			
			addInactiveTime(e.getKey(), expFinishTime - e.getValue());
			it.remove();
		}
	}

	protected void logReset(String expFile) {		
		String path = host.getLogPath();
		
		// Close any previous log if it was open
		if( experimentLog != null ) logFlush();
		
		String filename = String.format("%s/%s %d.log", path, expFile, clients.size());
		logger.info("Trying to open file " + filename);
		
		expStartTime = System.currentTimeMillis();
		
		try {
			experimentLog = new PrintWriter(new FileWriter(filename), true);
			logString(toString() + " started");
		} catch (IOException e) {
			logger.warning("Couldn't open log file " + filename + " for writing!");			
			e.printStackTrace();
			experimentLog = null;
		}		
	}
	
	protected synchronized void logString(String msg) {
		if( experimentLog != null) experimentLog.printf("%s %s\n", 
				Utils.clockStringMillis(System.currentTimeMillis() - expStartTime), msg);
		else System.out.println("Log discarded: " + msg);
	}
	
	protected void logFlush() {
		logString(toString() + " finished");
		expFinishTime = System.currentTimeMillis();
		
		try {
			experimentLog.flush();
			experimentLog.close();
		} catch(NullPointerException e) {
			e.printStackTrace();
		}
			
		experimentLog = null;
	}
	
	/**
	 * Send a message to a particular user
	 * @param sessionId
	 * @param data
	 */
	protected void sendServiceMsg(BigInteger sessionId, Object data) {
		host.bayeux.getSession(host.clientToId.inverse().get(sessionId)).deliver(
				expBroadcaster, Codec.expSvcPrefix + getChannelName(), data, null);
	}
	
	/**
	 * Broadcast a message to everyone
	 * @param data
	 */
	protected void sendBroadcastMsg(Object data) {		
		host.bayeux.getChannel(Codec.expChanPrefix + getChannelName()).publish(expBroadcaster, data, null);
	}
	
	/**
	 * service message from client 
	 * @param sessionId
	 * @param data
	 */
	protected abstract void rcvServiceMsg(BigInteger sessionId, Map<String, Object> data);
	
	/**
	 * 
	 * @param sessionId
	 * @param data
	 * @return true if the message should be relayed to other clients
	 */
	protected abstract boolean rcvBroadcastMsg(BigInteger sessionId, Map<String, Object> data);
	
	/**
	 * Puts the client in a connected time and records the time since last disconnection, if any
	 * @param id
	 */
	public void clientConnected(BigInteger id) {
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
	public void clientDisconnected(BigInteger id) {
		clients.put(id, false);
		
		// Record disconnect time
		disconnectTime.put(id, System.currentTimeMillis());
	}
	
	abstract public void run();
}
