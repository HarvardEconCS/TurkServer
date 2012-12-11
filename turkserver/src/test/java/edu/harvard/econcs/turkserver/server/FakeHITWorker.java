package edu.harvard.econcs.turkserver.server;

import java.net.InetAddress;
import java.util.Map;

import edu.harvard.econcs.turkserver.api.HITWorker;

public abstract class FakeHITWorker implements HITWorker {

	protected String hitId;
	protected String assignmentId;
	protected String workerId;
	protected String username;	
	
	FakeExperimentController cont;
	
	/*
	void setIds(String hitId, String assignmentId, String workerId, String username ) {
		this.hitId = hitId;
		this.assignmentId = assignmentId;
		this.workerId = workerId;
		this.username = username;
	}
	*/	
	
	protected void sendBroadcast(Map<String, Object> msg) {
		cont.rcvBroadcast(this, msg);
	}
	
	protected void sendPrivate(Map<String, Object> msg) {
		cont.rcvService(this, msg);
	}
	
	protected abstract void rcvBroadcast(Object msg);
	
	protected abstract void rcvPrivate(Object msg);
	
	@Override
	public String getHitId() {		
		return hitId;
	}

	@Override
	public String getAssignmentId() {		
		return assignmentId;
	}

	@Override
	public String getWorkerId() {		
		return workerId;
	}

	@Override
	public String getUsername() {		
		return username;
	}

	@Override
	public InetAddress getIPAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() {		
		return true;
	}

	@Override
	public int getLiveNumDisconnects() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getLiveInactivePercent() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLiveInactiveDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendExperimentPrivate(Object msg) throws MessageException {
		rcvPrivate(msg);
	}

	@Override
	public String toString() {
		return "Fake HITWorker with " + hitId;
	}
}
