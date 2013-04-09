package edu.harvard.econcs.turkserver.api;

import java.net.InetAddress;
import java.util.Map;

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
	 * Gets the number of milliseconds this worker has been disconnected.
	 * -1 if the worker is not disconnected.
	 * @return
	 */
	long getLastDisconnectedTime();
	
	/**
	 * Get the total amount of time a worker has been disconnected during an experiment (does not include inactive)
	 * @return
	 */
	long getTotalDisconnectedTime();

	/**
	 * Gets the actual number of disconnects since the worker has accepted the HIT.
	 * @return
	 */
	int getNumDisconnects();

	/**
	 * Get the length of the last inactivity segment (even if the worker is currently not inactive)
	 * @return
	 */
	long getLastInactiveTime();
	
	/**
	 * Get the total number of milliseconds inactive that have been tracked so far, including time disconnected
	 * @return
	 */
	long getTotalInactiveTime();
	
	/**
	 * Gets the amount of time inactive since the worker has started an experiment.
	 * @return
	 */
	double getInactivePercent();
	
	/**
	 * Gets a description of inactivity (subject to implementation)
	 * @return
	 */
	String getInactiveInfo();
	
	/**
	 * Send a JSON-encoded message to this particular user
	 * @param msg
	 */
	void deliverExperimentService(Map<String, Object> msg) throws MessageException;	
}
