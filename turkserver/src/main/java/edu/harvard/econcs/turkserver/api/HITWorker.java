package edu.harvard.econcs.turkserver.api;

import java.net.InetAddress;

import edu.harvard.econcs.turkserver.server.MessageException;

/**
 * Represents a HIT, possibly accepted by a worker
 * 
 * @author mao
 *
 */
public interface HITWorker {
	
	String getHitId();
	
	String getAssignmentId();
	
	String getWorkerId();
		
	/**
	 * Get the user name for a session
	 * User name doesn't change unless HIT returned, so value is stored
	 * @return user name or a descriptor
	 */
	String getUsername();
	
	InetAddress getIPAddress();
	
	boolean isConnected();
	
	/**
	 * Gets the actual number of disconnects since the worker has accepted the HIT.
	 * @return
	 */
	int getLiveNumDisconnects();
	
	/**
	 * Gets the amount of time inactive since the worker has started an experiment.
	 * @return
	 */
	double getLiveInactivePercent();
	
	/**
	 * Gets a description of inactivity (subject to implementation)
	 * @return
	 */
	String getLiveInactiveDescriptor();
	
	/**
	 * Send a JSON-encoded message to this particular user
	 * @param msg
	 */
	void sendExperimentPrivate(Object msg) throws MessageException;	
}
