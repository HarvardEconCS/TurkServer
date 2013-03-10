package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.schema.Quiz;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.HITWorkerImpl;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
@SuppressWarnings("unused")
@Singleton
public class MockDataTracker extends ExperimentDataTracker {		
	
	private final ConcurrentMap<String, Session> hitIdToSessions;
	
	private final Multimap<String, Session> workerIdToSessions;
	private final Multimap<String, Quiz> workerIdToQuizzes;
	
	// Experiment tracking - FALSE if in progress and TRUE if finished
	protected final ConcurrentMap<String, Experiment> experiments; 
	
	public MockDataTracker() {						
				
		hitIdToSessions = new ConcurrentHashMap<String, Session>();		
		
		// TODO double-check the concurrency of this if it becomes important										
		workerIdToSessions = Multimaps.synchronizedSetMultimap(HashMultimap.<String, Session>create());
		workerIdToQuizzes = Multimaps.synchronizedSetMultimap(HashMultimap.<String, Quiz>create());
		
		// Experiment trackers
		experiments = new ConcurrentHashMap<String, Experiment>();
	}

	@Override
	public boolean hitExistsInDB(String hitId) {
		return hitIdToSessions.containsKey(hitId);
	}

	@Override
	public synchronized SessionSummary getSetSessionSummary() {
		int created = 0;
		int assigned = 0;
		int completed = 0;
		
		for( Session s : hitIdToSessions.values() ) {
			if( SessionRecord.status(s) == SessionStatus.COMPLETED ) {
				completed++;
				assigned++;
				created++;
			}
			else if( SessionRecord.status(s) == SessionStatus.EXPERIMENT ||
					SessionRecord.status(s) == SessionStatus.LOBBY ||
					SessionRecord.status(s) == SessionStatus.ASSIGNED
					) {
				assigned++;
				created++;
			}
			else {
				created++;
			}				
		}
		return new SessionSummary(created, assigned, completed);
	}

	@Override
	public Collection<Experiment> getSetExperiments() {		
		return experiments.values();
	}

	@Override
	public Collection<Quiz> getSetQuizRecords(String workerId) {		
		return workerIdToQuizzes.get(workerId);
	}
	
	@Override
	public Collection<Session> getSetSessionInfoForWorker(String workerId) {
		return workerIdToSessions.get(workerId);	 					
	}

	@Override
	public Session getStoredSessionInfo(String hitId) {
		return hitIdToSessions.get(hitId); 
	}

	@Override
	public void saveSession(Session record) {
		hitIdToSessions.put(record.getHitId(), record);
				
		if( record.getWorkerId() != null )
			workerIdToSessions.put(record.getWorkerId(), record);
	}

	@Override
	public void saveHITId(String hitId) {
		Session s = new Session();
		s.setHitId(hitId);
		
		hitIdToSessions.put(hitId, s);
	}

	@Override
	public void saveWorkerAssignment(HITWorkerImpl worker,
			String assignmentId, String workerId) {
		Session s = worker.getSessionRecord();		
				
		s.setAssignmentId(assignmentId);
		s.setWorkerId(workerId);
		
		hitIdToSessions.put(s.getHitId(), s);		
		workerIdToSessions.put(workerId, s);
		
		logger.info(String.format("session %s has assignment %s by worker %s",
				s.getHitId(), assignmentId, workerId));
	}

	@Override
	public void saveQuizResults(String hitId, String workerId, Quiz qr) {		
		logger.info(String.format("Session %s got %d out of %d correct", 
				hitId, qr.getNumCorrect(), qr.getNumTotal()));
		
		// Correct, total, score, answers already set when parsed; we just add worker/sessionId		
		qr.setSessionId(hitId);
		qr.setWorkerId(workerId);
		qr.setSetId("");
		
		workerIdToQuizzes.put(workerId, qr);
	}

	@Override
	public void saveExitSurveyResults(HITWorkerImpl session, String comments) {
		super.saveExitSurveyResults(session, comments);
		
		logger.info(String.format("Session %s provided exit survey answers %s" , session, comments));				
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
	public void saveExperiment(HITWorkerImpl session, String experimentID) {
		super.saveExperiment(session, experimentID);				
		
		logger.info("{} joined experiment {}", session, experimentID);	
	}

	@Override
	protected void saveExpStartTime(String expId, int groupsize, String inputData, long startTime) {
		Timestamp start = new Timestamp(startTime);
		
		Experiment e = new Experiment();
		
		e.setId(expId);
		e.setParticipants(groupsize);
		e.setInputdata(inputData);
		e.setStartTime(start);
		
		experiments.put(expId, e);
		
		logger.info(
				String.format("%s(%d), %s, started at %s", expId, groupsize, inputData, start));		
	}

	@Override
	protected void saveExpRoundStart(String expId, int currentRound,
			long startTime) {
		logger.info(expId + " round " + currentRound + " started");		
	}

	@Override
	protected void saveExpRoundEnd(String expId, long endTime, int round, String roundLog) {
		logger.info(expId + " round " + round);
		logger.info(roundLog);		
	}

	@Override
	protected void saveExpEndInfo(String expId, long endTime, String logOutput) {
		Timestamp end = new Timestamp(endTime);
		
		Experiment e = experiments.get(expId);
				
		e.setEndTime(end);
		e.setResults(logOutput);
		
		logger.info(String.format(expId + " ended at " + end));		
	}

	@Override
	protected void saveSessionCompleteInfo(HITWorkerImpl session) {
		super.saveSessionCompleteInfo(session);
				
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
		
		workerIdToSessions.get(workerId).remove(s);
		
		logger.info(String.format("Worker %s disassociated with session %s", workerId, hitId));				
	}

	@Override
	public List<Session> expireUnusedSessions() {
		// TODO implement this if needed for testing
		logger.warn("Deleting expiring HITs not yet implemented in dummy tracker");
		
		List<Session> unused = new LinkedList<Session>();
		
		for( Session s : hitIdToSessions.values() ) {
			if( SessionRecord.status(s) == SessionStatus.UNASSIGNED || 
					SessionRecord.status(s) == SessionStatus.ASSIGNED )
				unused.add(s);
		}
		
		return unused;
	}

}
