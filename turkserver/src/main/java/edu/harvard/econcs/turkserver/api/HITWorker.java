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
	
	void sendExperimentMessage(Object msg);

	boolean isConnected();
	
	int getNumDisconnects();
	
	double getInactivePercent();
	
	String getInactiveDescriptor();
}
