package edu.harvard.econcs.turkserver.api;

import java.net.InetAddress;

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
	
	String getUsername();
	
	InetAddress getIPAddress();
	
	boolean isConnected();	
	
	void sendMessage(Object msg);
	
}
