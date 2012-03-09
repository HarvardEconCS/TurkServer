package edu.harvard.econcs.turkserver.server.mysql;


import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.TooManyFailsException;
import edu.harvard.econcs.turkserver.server.ExperimentServer;
import edu.harvard.econcs.turkserver.server.HostServer;
import edu.harvard.econcs.turkserver.server.SessionRecord;

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
import net.andrewmao.misc.Utils;

import org.apache.commons.collections.map.MultiValueMap;

/**
 * A dummy user tracker that does not store anything
 * and prints out database events
 * use for testing on the sandbox
 * @author mao
 *
 */
public class ExperimentDummyTracker extends ExperimentDataTracker {

	private final boolean requireQuiz;
	
	private ConcurrentBooleanCounter<BigInteger> usedIDs;
	
	private ConcurrentHashMap<BigInteger, String> idToAssignmentId;	
	
	private MultiValueMap workerIdToSessions;
	
	public ExperimentDummyTracker(int simultaneousSessionLimit, int totalSetLimit, boolean requireQuiz) {
		super(simultaneousSessionLimit, totalSetLimit);
		
		this.requireQuiz = requireQuiz;
		
		usedIDs = new ConcurrentBooleanCounter<BigInteger>();
				
		idToAssignmentId = new ConcurrentHashMap<BigInteger, String>();
		
		// TODO double-check the concurrency of this if it becomes important
		workerIdToSessions = MultiValueMap.decorate(
				new ConcurrentHashMap<String, BigInteger>(), ConcurrentLinkedQueue.class);
	}
	
	@Override
	public BigInteger getNewSessionID() {
		BigInteger bi = null;
		do {
			bi = new BigInteger(HostServer.ID_LEN, rnd);
		} while	(usedIDs.contains(bi));
		
		logger.info("New session ID created: " + bi.toString(16));
		
		usedIDs.put(bi, false);		
		return bi;
	}

	@Override
	public boolean sessionExistsInDB(BigInteger sessionID) {
		return usedIDs.containsKey(sessionID);
	}

	@Override
	protected boolean userRequiresQuiz(String workerId)
			throws TooManyFailsException {		
		return requireQuiz;
	}

	@Override
	public boolean sessionCompletedInDB(BigInteger id) {		
		return (usedIDs.containsKey(id) && (usedIDs.get(id) == true));
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SessionRecord> getSetSessionInfoForWorker(String workerId) {		
		Collection<BigInteger> sessions =
			(Collection<BigInteger>) workerIdToSessions.get(workerId);
		
		List<SessionRecord> srs = new ArrayList<SessionRecord>(sessions.size());		
		for( BigInteger bi : sessions ) srs.add(getStoredSessionInfo(bi));
		
		return srs;
	}

	@Override
	public SessionRecord getStoredSessionInfo(BigInteger sessionID) {
		// Obviously, this is missing most of the stuff, but who cares for now
		SessionRecord sr = new SessionRecord();
		
		sr.setId(sessionID);
		sr.setAssignmentId(idToAssignmentId.get(sessionID));
		
		return sr; 
	}

	@Override
	public void saveHITIdForSession(BigInteger sessionID, String hitId) {		
		logger.info(String.format("session %s assigned to HIT %s\n", sessionID.toString(16), hitId));		
	}

	@Override
	public void saveAssignmentForSession(BigInteger sessionID,
			String assignmentId, String workerId) {
		idToAssignmentId.put(sessionID, assignmentId);		
		workerIdToSessions.put(workerId, sessionID);
		
		logger.info(String.format("session %s has assignment %s by worker %s\n",
				sessionID.toString(16), assignmentId, workerId));
	}

	@Override
	public void saveQuizResults(BigInteger sessionID, String workerId,
			QuizResults qr) {
		logger.info(String.format("Session %s got %d out of %d correct", 
				sessionID.toString(16), qr.correct, qr.total));
	}

	@Override
	protected void saveUsernameForSession(BigInteger sessionId, String username) {		
		logger.info(String.format("test: %s registered username '%s'", 
				sessionId.toString(16), username));
	}

	@Override
	public void saveIPForSession(BigInteger id, InetAddress remoteAddress, Date lobbyTime) {
		logger.info(id.toString(16) + " connected from IP " + remoteAddress.getHostAddress() +
				" at time " + lobbyTime.toString());		
	}

	@Override
	protected void saveExpStartTime(ExperimentServer<?> exp, Date startTime) {
		logger.info(String.format(exp + " started at " + Utils.getTimeStringFromDate(startTime)));		
	}

	@Override
	protected void saveExperimentForSession(BigInteger clientID,
			String experimentID) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void saveExpEndTime(ExperimentServer<?> exp, Date endTime) {
		logger.info(String.format(exp + " ended at " + Utils.getTimeStringFromDate(endTime)));		
	}

	@Override
	protected void saveSessionCompleteInfo(BigInteger sessionID,
			double inactivePercent) {
		usedIDs.put(sessionID, true);
		logger.info(String.format("session %s was inactive for fraction %.02f",
				sessionID.toString(16), inactivePercent));		
	}

	
	@SuppressWarnings("rawtypes")
	@Override
	protected void clearWorkerForSession(BigInteger id) {
		// TODO Make this more efficient, although it is so in the DB implementation
		
		for( Object worker : workerIdToSessions.keySet() ) {
			if( ((Collection) workerIdToSessions.get(worker)).contains(id) ) {
				workerIdToSessions.remove(worker, id);
				logger.info(String.format("Worker %s disassociated with session %s", 
						worker.toString(), id.toString(16)));
				break;
			}			
		}
	}

	@Override
	public List<SessionRecord> expireUnusedSessions() {
		logger.warning("Expiring HITs not yet implemented in dummy tracker");
		return new LinkedList<SessionRecord>();
	}

}
