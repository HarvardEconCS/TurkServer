package edu.harvard.econcs.turkserver.server;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.cometd.bayeux.server.ServerSession;

import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.schema.Session;

public class HITWorkerImpl implements HITWorker, HITWorkerGroup {

	volatile WeakReference<ServerSession> cometdSession;
	volatile Session record;
	volatile ExperimentControllerImpl expCont;
	
	final AtomicInteger numDisconnects = new AtomicInteger();
	
	final AtomicReference<Long> lastDisconnectTime = new AtomicReference<Long>();
	final AtomicReference<Long> lastInactiveStart = new AtomicReference<Long>();
	
	final AtomicLong pastInactiveMillis = new AtomicLong();
	final AtomicLong currentInactiveMillis = new AtomicLong();
	
	public HITWorkerImpl(ServerSession cometdSession, Session dbSession) {
		this.cometdSession = new WeakReference<ServerSession>(cometdSession);
		this.record = dbSession;				
	}
	
	void setServerSession(ServerSession newSession) {
		cometdSession = new WeakReference<ServerSession>(newSession);
	}
	
	void setExperiment(ExperimentControllerImpl cont) {
		this.expCont = cont;
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
		String username = record.getUsername();				 
		if( username != null ) return username;
		
		return "User for " + record.getHitId();
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
	public long getDisconnectedTime() {
		Long lastDisc = lastDisconnectTime.get();
		
		if( lastDisc == null )
			return -1;
		else
			return System.currentTimeMillis() - lastDisc;
	}

	@Override
	public void deliverExperimentService(Map<String, Object> msg) throws MessageException {
		// Find the experiment this user is on and send a message on that channel
		expCont.sendExperimentService(this, msg);
	}
	
	public void deliverUserService(Map<String, Object> msg) throws MessageException {
		ServerSession session = cometdSession.get();
		if( session == null || !session.isConnected() ) throw new MessageException();
		
		session.deliver(session, "/service/user", msg, null);
	}

	/**
	 * call when this worker has disconnected previous and reconnects
	 * records the time since last disconnection, if any
	 */
	void reconnected() {
		Long lastDisc = lastDisconnectTime.getAndSet(null);
		
		if( lastDisc == null ) {
			System.out.println("reconnected but don't have record of last disconnect");
			return;
		}
		
		/*
		 * Add disconnected time to total inactive time
		 * 
		 * TODO this doesn't take care of the case where inactive is double-counted
		 * after a non-reloading reconnect
		 */
		
		pastInactiveMillis.addAndGet(System.currentTimeMillis() - lastDisc);			
	}
	
	/**
	 * call when this worker disconnects
	 * records the time
	 */
	void disconnected() {
		lastDisconnectTime.set(System.currentTimeMillis());
		numDisconnects.incrementAndGet();
	}
	
	/**
	 * Add milliseconds of inactive time
	 * @param millis
	 * @param inactiveTime 
	 */
	void addInactiveTime(long inactiveStart, long inactiveTime) {
		Long lastStart = lastInactiveStart.get();
		
		if( lastStart == null ) {
			currentInactiveMillis.set(inactiveTime);
			lastInactiveStart.set(inactiveStart);
		}
		else if( lastStart == inactiveStart ) {
			currentInactiveMillis.set(inactiveTime);			
		}
		else {
			// New inactive segment, add previous segment to total
			long pastMillis = currentInactiveMillis.getAndSet(inactiveTime);
			lastInactiveStart.set(inactiveStart);
		
			pastInactiveMillis.addAndGet(pastMillis);
		}			
	}	
	
	/**
	 * Call at the end of an experiment to finish any uncomputed values
	 */
	public void finalizeActivity() {		
		
		if( !isConnected() ) {
			Long lastDisc = lastDisconnectTime.get();
			
			if( lastDisc == null ) {
				System.out.println("not connected but don't have record of last disconnect");
				return;
			}
			else if( lastDisc > expCont.expFinishTime ) {
				System.out.println("ignoring last disconnect after experiment end");
				return;
			}
			
			addInactiveTime(lastDisc, expCont.expFinishTime - lastDisc);
		}
		else {
			long pastMillis = currentInactiveMillis.getAndSet(0);
			lastInactiveStart.set(null);
			
			pastInactiveMillis.addAndGet(pastMillis);
		}
	}
	
	@Override
	public int getNumDisconnects() {		
		return numDisconnects.get();
	}

	@Override
	public long getLastInactiveTime() {		
		return currentInactiveMillis.get();
	}

	@Override
	public long getTotalInactiveTime() {
		return pastInactiveMillis.get() + currentInactiveMillis.get();
	}

	@Override
	public double getInactivePercent() {
		if( expCont == null ) return 0d;
			
		long totalTime = System.currentTimeMillis() - expCont.expStartTime;
		return 1.0d * getTotalInactiveTime() / totalTime;
	}

	@Override
	public String getInactiveInfo() {		
		return String.format("Inactive a total of %d secs, last inactive %d secs",
				getTotalInactiveTime() / 1000, getLastInactiveTime() / 1000);
	}
	
	@Override
	public int groupSize() {		
		return 1;
	}

	@Override
	public boolean contains(HITWorker hitWorker) {		
		return this.equals(hitWorker);
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
