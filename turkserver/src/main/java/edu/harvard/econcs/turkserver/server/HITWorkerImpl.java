package edu.harvard.econcs.turkserver.server;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.cometd.bayeux.server.ServerSession;

import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.schema.Session;

public class HITWorkerImpl implements HITWorker, HITWorkerGroup {

	volatile WeakReference<ServerSession> cometdSession;
	volatile Session record;
	volatile ExperimentControllerImpl expCont;
	
	public HITWorkerImpl(ServerSession cometdSession, Session dbSession) {
		this.cometdSession = new WeakReference<ServerSession>(cometdSession);
		this.record = dbSession;
	}
	
	void setServerSession(ServerSession newSession) {
		cometdSession = new WeakReference<ServerSession>(newSession);
	}
	
	void setExperiment(ExperimentControllerImpl cont) {
		this.expCont = expCont;
	}	
	
	@Override
	public String getHitId() {		
		return record.getHitId();
	}

	@Override
	public String getAssignmentId() {		
		return record.getAssignmentId();
	}

	@Override
	public String getWorkerId() {		
		return record.getWorkerId();
	}

	@Override
	public String getUsername() {		
		return record.getUsername();
	}

	@Override
	public InetAddress getIPAddress() {		
		try {
			String ip = record.getIpAddr();
			if (ip == null) return null;
			return InetAddress.getByName(ip);
		} catch (UnknownHostException e) {			
			return null;
		}
	}

	@Override
	public boolean isConnected() {		
		ServerSession session = cometdSession.get();
		if( session == null ) return false;
		
		return session.isConnected();
	}

	@Override
	public void sendExperimentPrivate(Object msg) throws MessageException {
		// Find the experiment this user is on and send a message on that channel
		expCont.sendExperimentService(this, msg);
	}
	
	public void sendServiceMessage(Object msg) throws MessageException {
		ServerSession session = cometdSession.get();
		if( session == null || !session.isConnected() ) throw new MessageException();
		
		session.deliver(session, "/service/user", msg, null);
	}

	public void addInactiveTime(long inactiveTime) {
		// TODO Auto-generated method stub		
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
	public int groupSize() {		
		return 1;
	}

	@Override
	public List<HITWorker> getHITWorkers() {		
		return Collections.singletonList((HITWorker) this);
	}

	@Override
	public List<String> getHITIds() {		
		return Collections.singletonList( this.getHitId() );		
	}

	@Override
	public List<String> getWorkerIds() {		
		return Collections.singletonList( this.getWorkerId() );
	}

	@Override
	public HITWorker findByHITId(String hitId) {
		if( hitId == null || !hitId.equals(this.getHitId())) 
			return null;
		
		return this;
	}

	public Session getSessionRecord() {
		return record;
	}

	@Override
	public boolean equals(Object other) {
		// TODO: not sure if we want to implement things this way 
		
		if( ! (other instanceof HITWorker) ) return false;		
		
		HITWorker otherhw = (HITWorker) other;
		
		String thisHIT = record.getHitId();
		String otherHIT = otherhw.getHitId();
		String thisWorker = record.getWorkerId();
		String otherWorker = otherhw.getWorkerId();
		
		if( thisHIT == null || otherHIT == null ) return false;
		if( thisWorker == null || otherWorker == null ) return false;
		
		return thisHIT.equals(otherHIT) && thisWorker.equals(otherWorker);
	}
	
	public String toString() {
		return String.format("%s @ HIT %s", getWorkerId(), getHitId());
	}
	
}
