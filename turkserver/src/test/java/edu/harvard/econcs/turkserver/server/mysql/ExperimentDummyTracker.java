package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.schema.Quiz;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.ExperimentControllerImpl;
import edu.harvard.econcs.turkserver.server.HITWorkerImpl;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.andrewmao.misc.ConcurrentBooleanCounter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.inject.Singleton;

/**
 * A dummy user tracker that does not store anything
 * and prints out database events
 * use for testing on the sandbox
 * @author mao
 *
 */
@Singleton
public class ExperimentDummyTracker extends ExperimentDataTracker {		
	
	private final ConcurrentBooleanCounter<String> usedIDs;
	
	private final ConcurrentMap<String, Session> hitIdToSessions;		
	private final Multimap<String, Session> workerIdToSessions;
	
	// Experiment tracking - FALSE if in progress and TRUE if finished
	protected final ConcurrentBooleanCounter<String> experiments; 
	
	public ExperimentDummyTracker() {				
		
		usedIDs = new ConcurrentBooleanCounter<String>();
				
		hitIdToSessions = new ConcurrentHashMap<String, Session>();		
		
		// TODO double-check the concurrency of this if it becomes important		
		@SuppressWarnings("unused")
		SetMultimap<String, Session> temp;
		workerIdToSessions = Multimaps.synchronizedSetMultimap(temp = HashMultimap.create()); 				
		
		// Experiment trackers
		experiments = new ConcurrentBooleanCounter<String>();
	}
	
	@Override
	public void newExperimentStarted(ExperimentControllerImpl cont) {
		experiments.put(cont.getExpId(), false);
		
		super.newExperimentStarted(cont);
	}
	
	@Override
	public void experimentFinished(ExperimentControllerImpl cont) {
		super.experimentFinished(cont);
		
		experiments.put(cont.getExpId(), true);
	}

	@Override
	protected void saveSession(Session record) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hitExistsInDB(String sessionID) {
		return usedIDs.containsKey(sessionID);
	}

	@Override
	public List<Experiment> getSetExperiments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Quiz> getSetQuizRecords(String workerId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Session> getSetSessionInfoForWorker(String workerId) {
		return new ArrayList<Session>(workerIdToSessions.get(workerId));	 					
	}

	@Override
	public Session getStoredSessionInfo(String hitId) {
		return hitIdToSessions.get(hitId); 
	}

	@Override
	public void saveHITId(String hitId) {
		usedIDs.put(hitId, false);
	}

	@Override
	public void saveAssignmentForSession(String hitId,
			String assignmentId, String workerId) {
		Session s = hitIdToSessions.get(hitId);
		if( s == null ) s = new Session();
		
		s.setHitId(hitId);
		s.setAssignmentId(assignmentId);
		s.setWorkerId(workerId);
		
		hitIdToSessions.put(hitId, s);		
		workerIdToSessions.put(workerId, s);
		
		logger.info(String.format("session %s has assignment %s by worker %s",
				hitId, assignmentId, workerId));
	}

	@Override
	public void saveQuizResults(String hitId, String workerId, QuizResults qr) {		
		logger.info(String.format("Session %s got %d out of %d correct", 
				hitId, qr.correct, qr.total));
	}

	@Override
	public void saveUsername(HITWorkerImpl session, String username) {
		super.saveUsername(session, username);
		
		logger.info(String.format("test: %s registered username '%s'", 
				session, username));
	}

	@Override
	public void saveIP(HITWorkerImpl session, InetAddress remoteAddress, Date lobbyTime) {
		super.saveIP(session, remoteAddress, lobbyTime);
		
		logger.info(session + " connected from IP " + remoteAddress.getHostAddress() +
				" at time " + lobbyTime.toString());		
	}

	@Override
	protected void saveExpStartTime(String expId, int groupsize, String inputData, long startTime) {
		logger.info(
				String.format("%s(%d), %s, started at %s", expId, groupsize, inputData, new Date(startTime)));		
	}

	@Override
	protected void saveExperiment(HITWorkerImpl session, String experimentID) {
		super.saveExperiment(session, experimentID);
		
		logger.info("{} joined experiment {}", session, experimentID);	
	}

	@Override
	protected void saveExpEndTime(String expId, long endTime) {
		logger.info(String.format(expId + " ended at " + new Date(endTime)));		
	}

	@Override
	protected void saveSessionCompleteInfo(HITWorkerImpl session) {
		super.saveSessionCompleteInfo(session);
		
		usedIDs.put(session.getHitId(), true);
		logger.info(String.format("session %s was inactive for fraction %.02f",
				session, session.getLiveInactivePercent()));		
	}
		
	@Override
	public void clearWorkerForSession(String hitId) {				
		Session s = hitIdToSessions.get(hitId);
		if( s == null ) return;
		
		String workerId = s.getWorkerId();
		s.setWorkerId(null);
		s.setUsername(null);
		
		logger.info(String.format("Worker %s disassociated with session %s", workerId, hitId));				
	}

	@Override
	public List<Session> expireUnusedSessions() {
		logger.warn("Expiring HITs not yet implemented in dummy tracker");
		return new LinkedList<Session>();
	}

}
