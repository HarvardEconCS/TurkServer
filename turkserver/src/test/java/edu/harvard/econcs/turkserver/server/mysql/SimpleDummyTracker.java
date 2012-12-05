package edu.harvard.econcs.turkserver.server.mysql;

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
import edu.harvard.econcs.turkserver.schema.Session;

@Deprecated
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
	public List<Session> getSetSessionInfoForWorker(String workerId) {
		Collection<String> sessions =
				(Collection<String>) workerIdToSessions.get(workerId);
		if( sessions == null ) return null;
		
		List<Session> srs = new ArrayList<Session>(sessions.size());		
		for( String bi : sessions ) srs.add(getStoredSessionInfo(bi));

		return srs;
	}

	@Override
	public List<Object[]> getWorkerAndTotalDataCounts(String workerId) {
		logger.warning("Tracking set data not yet implemented in dummy tracker");
		return null;
	}

	@Override
	public Session getStoredSessionInfo(String sessionID) {
		// Obviously, this is missing most of the stuff, but who cares for now
		Session sr = new Session();
		
		sr.setHitId(sessionID);
		sr.setAssignmentId(idToAssignmentId.get(sessionID));		
		
		return sr; 
	}

	@Override
	public void saveHITId(String hitId) {
		usedIDs.put(hitId, false);
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
	public List<Session> expireUnusedSessions() {
		logger.warning("Expiring HITs not yet implemented in dummy tracker");
		return new LinkedList<Session>();
	}

}
