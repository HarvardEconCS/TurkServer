package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.schema.Quiz;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.HITWorkerImpl;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Collection;
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
	
	private final ConcurrentMap<String, Session> hitIdToSessions;		
	private final Multimap<String, Session> workerIdToSessions;
	
	// Experiment tracking - FALSE if in progress and TRUE if finished
	protected final ConcurrentMap<String, Experiment> experiments; 
	
	public ExperimentDummyTracker() {						
				
		hitIdToSessions = new ConcurrentHashMap<String, Session>();		
		
		// TODO double-check the concurrency of this if it becomes important		
		@SuppressWarnings("unused")
		SetMultimap<String, Session> temp;
		workerIdToSessions = Multimaps.synchronizedSetMultimap(temp = HashMultimap.create()); 				
		
		// Experiment trackers
		experiments = new ConcurrentHashMap<String, Experiment>();
	}

	@Override
	public boolean hitExistsInDB(String hitId) {
		return hitIdToSessions.containsKey(hitId);
	}

	@Override
	public Collection<Experiment> getSetExperiments() {		
		return experiments.values();
	}

	@Override
	public Collection<Quiz> getSetQuizRecords(String workerId) {
		// TODO Auto-generated method stub
		return null;
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
	protected void saveSession(Session record) {
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
	protected void saveExperiment(HITWorkerImpl session, String experimentID) {
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
	protected void saveExpEndTime(String expId, long endTime) {
		Timestamp end = new Timestamp(endTime);
		
		Experiment e = experiments.get(expId);
				
		e.setEndTime(end);
		
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
		
		logger.info(String.format("Worker %s disassociated with session %s", workerId, hitId));				
	}

	@Override
	public List<Session> expireUnusedSessions() {
		logger.warn("Expiring HITs not yet implemented in dummy tracker");
		return new LinkedList<Session>();
	}

}
