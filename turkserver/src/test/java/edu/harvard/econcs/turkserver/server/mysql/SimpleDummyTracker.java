package edu.harvard.econcs.turkserver.server.mysql;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.andrewmao.misc.ConcurrentBooleanCounter;

import org.apache.commons.collections.map.MultiValueMap;

import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.server.HostServer;
import edu.harvard.econcs.turkserver.server.SessionRecord;

public class SimpleDummyTracker extends SimpleDataTracker {

	private ConcurrentBooleanCounter<String> usedIDs;	
	private ConcurrentHashMap<String, String> idToAssignmentId;		
	private MultiValueMap workerIdToSessions;
	
	public SimpleDummyTracker(int simultaneousSessionLimit, int totalSetLimit) {
		super(simultaneousSessionLimit, totalSetLimit);
		
		usedIDs = new ConcurrentBooleanCounter<String>();		
		idToAssignmentId = new ConcurrentHashMap<String, String>();
		
		// TODO double-check the concurrency of this if it becomes important
		workerIdToSessions = MultiValueMap.decorate(
				new ConcurrentHashMap<String, String>(), ConcurrentLinkedQueue.class);
	}

	@Override
	public String getNewSessionID() {
		BigInteger bi = null;
		do {
			bi = new BigInteger(HostServer.ID_LEN, rnd);
		} while	(usedIDs.contains(bi));
		
		logger.info("New session ID created: " + bi.toString(16));
		
		usedIDs.put(bi.toString(16), false);		
		return bi.toString(16);
	}
	
	@Override
	public boolean sessionExistsInDB(String sessionID)
			throws SessionExpiredException {
		return usedIDs.containsKey(sessionID);
	}

	@Override
	public boolean sessionCompletedInDB(String id) {
		return (usedIDs.containsKey(id) && (usedIDs.get(id) == true));
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SessionRecord> getSetSessionInfoForWorker(String workerId) {
		Collection<String> sessions =
				(Collection<String>) workerIdToSessions.get(workerId);
		if( sessions == null ) return null;
		
		List<SessionRecord> srs = new ArrayList<SessionRecord>(sessions.size());		
		for( String bi : sessions ) srs.add(getStoredSessionInfo(bi));

		return srs;
	}

	@Override
	public List<Object[]> getWorkerAndTotalDataCounts(String workerId) {
		logger.warning("Tracking set data not yet implemented in dummy tracker");
		return null;
	}

	@Override
	public SessionRecord getStoredSessionInfo(String sessionID) {
		// Obviously, this is missing most of the stuff, but who cares for now
		SessionRecord sr = new SessionRecord();
		
		sr.setHitId(sessionID);
		sr.setAssignmentId(idToAssignmentId.get(sessionID));		
		
		return sr; 
	}

	@Override
	public void saveHITIdForSession(String nullStr, String hitId) {
		logger.info(String.format("Tracked HIT %s\n", hitId));
	}

	@Override
	public void saveAssignmentForSession(String hitId,
			String assignmentId, String workerId) {
		idToAssignmentId.put(hitId, assignmentId);		
		workerIdToSessions.put(workerId, hitId);
		
		logger.info(String.format("session %s has assignment %s by worker %s\n",
				hitId, assignmentId, workerId));
	}

	@Override
	public void setSessionData(String hitId, String Data) {
		logger.warning("Saving data not yet implemented in dummy tracker");		
	}

	@Override
	public void saveSessionLog(String hitId, String data) {
		System.out.println("Log for " + hitId);
		System.out.println(data);
	}

	@Override
	public void saveIPForSession(String hitId, InetAddress remoteAddress,
			Date lobbyTime) {
		logger.info(hitId + " connected from IP " + remoteAddress.getHostAddress() +
				" at time " + lobbyTime.toString());		
	}

	@Override
	public List<SessionRecord> expireUnusedSessions() {
		logger.warning("Expiring HITs not yet implemented in dummy tracker");
		return new LinkedList<SessionRecord>();
	}

}
