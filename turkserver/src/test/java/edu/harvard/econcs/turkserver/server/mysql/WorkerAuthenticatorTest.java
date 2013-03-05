package edu.harvard.econcs.turkserver.server.mysql;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.SessionCompletedException;
import edu.harvard.econcs.turkserver.SessionOverlapException;
import edu.harvard.econcs.turkserver.SimultaneousSessionsException;
import edu.harvard.econcs.turkserver.TooManySessionsException;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.WorkerAuthenticator;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

public class WorkerAuthenticatorTest {

	int concurrentSessionLimit = 1;
	int totalSetLimit = 1;
	String special_worker = "specialWorkerId";
	
	ExperimentDataTracker tracker;
	WorkerAuthenticator workerAuth;
	
	@Before
	public void setUp() throws Exception {
		List<String> specialWorkers = Collections.singletonList(special_worker);
		
		tracker = new ExperimentDummyTracker();
		
		Configuration conf = new PropertiesConfiguration();
		
		conf.addProperty(TSConfig.CONCURRENCY_LIMIT, concurrentSessionLimit);
		conf.addProperty(TSConfig.EXP_REPEAT_LIMIT, totalSetLimit);
		
		workerAuth = new WorkerAuthenticator(
				tracker,
				null,
				null,
				conf);
		
		workerAuth.setSpecialWorkers(specialWorkers);
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void testHITValidEmpty() throws Exception {
		String hitId = "testHITId";
		workerAuth.checkHITValid(hitId, "testWorkerId", tracker.getStoredSessionInfo(hitId));		
	}
	
	@Test
	public void testHITValidExisting() throws Exception {
		String hitId = "testHITId";
		tracker.saveHITId(hitId);
		workerAuth.checkHITValid(hitId, "testWorkerId", tracker.getStoredSessionInfo(hitId));		
	}
	
	@Test(expected=SessionOverlapException.class)
	public void testHITValidExperimentOverlap() throws Exception {
		String hitId = "testHITId";
		
		Session s = new Session();		
		s.setHitId(hitId);
		s.setWorkerId("someOtherWorker");
		// Signals started experiment
		s.setExperimentId("random experiment");
		
		assertEquals(SessionStatus.EXPERIMENT, SessionRecord.status(s));
		
		tracker.saveSession(s);
		
		workerAuth.checkHITValid(hitId, "thisWorker", tracker.getStoredSessionInfo(hitId));				
	}
	
	@Test(expected=SessionOverlapException.class)
	public void testHITValidCompletedOverlap() throws Exception {
		String hitId = "testHITId";
		
		Session s = new Session();		
		s.setHitId(hitId);
		s.setWorkerId("someOtherWorker");
		// Signals completed experiment
		s.setInactivePercent(0.00);
		
		assertEquals(SessionStatus.COMPLETED, SessionRecord.status(s));
		
		tracker.saveSession(s);
		
		workerAuth.checkHITValid(hitId, "thisWorker", tracker.getStoredSessionInfo(hitId));				
	}
	
	@Test(expected=SessionCompletedException.class)
	public void testHITValidCompleted() throws Exception {
		String hitId = "testHITId";
		String workerId = "workerId";
		
		Session s = new Session();		
		s.setHitId(hitId);
		s.setWorkerId(workerId);
		s.setInactivePercent(0.00);
		
		tracker.saveSession(s);
		
		workerAuth.checkHITValid(hitId, workerId, tracker.getStoredSessionInfo(hitId));				
	}
	
	@Test
	public void testLimitNone() throws Exception {
		String hitId = "testHITId";
		String workerId = "testWorkerId";		
		
		workerAuth.checkWorkerLimits(hitId, workerId, tracker.getStoredSessionInfo(hitId));		
	}
	
	@Test(expected=SimultaneousSessionsException.class)
	public void testLimitConcurrent() throws Exception {
		String hitId = "testHITId";
		String hitId2 = "testHITId2";
		
		String workerId = "testWorkerId";
		
		String assignmentId = "testAssignmentId";		
		
		Session takenSession = new Session();
		takenSession.setHitId(hitId);
		takenSession.setWorkerId(workerId);
		takenSession.setAssignmentId(assignmentId);		
		
		assertEquals(SessionStatus.ASSIGNED, SessionRecord.status(takenSession));
		
		tracker.saveSession(takenSession);
		
		// Attempt to take new session
		workerAuth.checkWorkerLimits(hitId2, workerId, tracker.getStoredSessionInfo(hitId2));
	}
	
	@Test(expected=TooManySessionsException.class)
	public void testLimitSets() throws Exception {
		String hitId = "testHITId";
		String hitId2 = "testHITId2";
		
		String workerId = "testWorkerId";
		
		String assignmentId = "testAssignmentId";
		
		Session s = new Session();
		s.setHitId(hitId);
		s.setWorkerId(workerId);
		s.setAssignmentId(assignmentId);
		// Current hack to signify completed session
		s.setInactivePercent(0.0);
		
		assertEquals(SessionStatus.COMPLETED, SessionRecord.status(s));
		
		tracker.saveSession(s);
		
		workerAuth.checkWorkerLimits(hitId2, workerId, tracker.getStoredSessionInfo(hitId2));
	}

}
