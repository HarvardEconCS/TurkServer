package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.cometd.MockServerSession;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.mturk.FakeHITController;
import edu.harvard.econcs.turkserver.schema.Quiz;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;
import edu.harvard.econcs.turkserver.server.mysql.MockDataTracker;

@SuppressWarnings("unchecked")
public class GroupServerTest {

	static final String USER_CHANNEL = "/service/user";
	
	MockDataTracker tracker; 
	GroupServer server;
	
	@Before
	public void setUp() throws Exception {
		
		tracker = new MockDataTracker();
		
		Configuration conf = TSConfig.getDefault();
		conf.setProperty(TSConfig.EXP_REPEAT_LIMIT, 1);
		conf.setProperty(TSConfig.SERVER_HITGOAL, 10);
		
		QuizFactory qf = new QuizFactory.NullQuizFactory();
		QuizPolicy onePassQuiz = new QuizPolicy.PercentageQuizPolicy(1d, 1);
		
		WorkerAuthenticator workerAuth = new WorkerAuthenticator(tracker, qf, onePassQuiz, conf);
		
		server = new GroupServer(			
				tracker,
				new FakeHITController(),
				workerAuth,
				new Experiments(null, null, null, tracker, null),			
				conf,
				new Lobby.NullLobby()
				);		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testHITView() {
		MockServerSession session = new MockServerSession();
		String hitId = "someHitId";
		
		assertFalse(tracker.hitExistsInDB(hitId));
		
		server.sessionView(session, hitId);
		
		// Verify that this hitId is stored now
		assertTrue(tracker.hitExistsInDB(hitId));
	}
		
	@Test
	public void testAcceptOverlap() {		
		String hitId = "testHITId";
		
		Session s = new Session();		
		s.setHitId(hitId);
		s.setWorkerId("someOtherWorker");		
		s.setExperimentId("random experiment"); // Means worker is in experiment
		
		assertEquals(SessionStatus.EXPERIMENT, SessionRecord.status(s));
		
		tracker.saveSession(s);
				
		MockServerSession conn = new MockServerSession();
		
		server.sessionAccept(conn, hitId, "someAssignment", "thisWorker");
		
		assertEquals(conn.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_sessionoverlap, ((Map<String, Object>) conn.lastData).get("status"));
	}

	@Test
	public void testAcceptSimultaneousLimit() {
		String hitId = "testHITId";	
		String workerId = "testWorkerId";		
		String assignmentId = "testAssignmentId";		
		
		Session takenSession = new Session();
		takenSession.setHitId(hitId);
		takenSession.setWorkerId(workerId);
		takenSession.setAssignmentId(assignmentId);		
		
		assertEquals(SessionStatus.ASSIGNED, SessionRecord.status(takenSession));
		
		tracker.saveSession(takenSession);

		String hitId2 = "testHITId2";		
		String assignmentId2 = "assignmentId2";
		MockServerSession conn = new MockServerSession();
		
		// Attempt to take new session
		server.sessionAccept(conn, hitId2, assignmentId2, workerId);
		
		assertEquals(conn.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_simultaneoussessions, ((Map<String, Object>) conn.lastData).get("status"));
	}
	
	@Test
	public void testAcceptHITLimit() {
		String hitId = "testHITId";				
		String workerId = "testWorkerId";		
		String assignmentId = "testAssignmentId";
		
		Session s = new Session();
		s.setHitId(hitId);
		s.setWorkerId(workerId);
		s.setAssignmentId(assignmentId);		
		s.setInactivePercent(0.0); // Identifies completed session
		
		assertEquals(SessionStatus.COMPLETED, SessionRecord.status(s));
		
		tracker.saveSession(s);
		
		String hitId2 = "testHITId2";
		String assignmentId2 = "assignmentId2";
		MockServerSession conn = new MockServerSession();
		
		// Attempt to take new session
		server.sessionAccept(conn, hitId2, assignmentId2, workerId);
		
		assertEquals(conn.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_toomanysessions, ((Map<String, Object>) conn.lastData).get("status"));
	}
	
	@Test
	public void testAcceptQuizRequired() {						
		MockServerSession conn = new MockServerSession();
		
		server.sessionAccept(conn, "hitId", "assignmentId", "workerId");
		
		assertEquals(conn.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_quizneeded, ((Map<String, Object>) conn.lastData).get("status"));
	}
	
	@Test
	public void testAcceptQuizFailsauce() {
		String hitId = "testHITId";				
		String workerId = "testWorkerId";		
		String assignmentId = "testAssignmentId";
		
		Quiz failedQuiz = new Quiz();		
		failedQuiz.setNumCorrect(0);
		failedQuiz.setNumTotal(1);
		failedQuiz.setScore(0d);
		
		tracker.saveQuizResults(hitId, workerId, failedQuiz);
		
		MockServerSession conn = new MockServerSession();
		
		server.sessionAccept(conn, hitId, assignmentId, workerId);
		
		assertEquals(conn.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_failsauce, ((Map<String, Object>) conn.lastData).get("status"));
	}
	
	@Test
	public void testAcceptLobby() {
		String hitId = "testHITId";				
		String workerId = "testWorkerId";		
		String assignmentId = "testAssignmentId";
		
		Quiz passedQuiz = new Quiz();		
		passedQuiz.setNumCorrect(1);
		passedQuiz.setNumTotal(1);
		passedQuiz.setScore(1d);
		
		tracker.saveQuizResults(hitId, workerId, passedQuiz);
		
		MockServerSession conn = new MockServerSession();
		
		server.sessionAccept(conn, hitId, assignmentId, workerId);
		
		assertEquals(conn.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_connectlobby, ((Map<String, Object>) conn.lastData).get("status"));
	}
	
	@Test
	public void testAcceptCompleted() {
		String hitId = "testHITId";
		String assignmentId = "testAssignmentId";
		String workerId = "workerId";
		
		Session s = new Session();		
		s.setHitId(hitId);
		s.setWorkerId(workerId);
		s.setInactivePercent(0.00);
		
		tracker.saveSession(s);
		
		// Pretend that we have a previous HITWorker for this guy
		HITWorkerImpl hitw = new HITWorkerImpl(null, s);
		server.hitWorkerTable.put(hitId, workerId, hitw);
		
		assertEquals(0, server.completedHITs);
		assertEquals(SessionStatus.COMPLETED, SessionRecord.status(s));		
		
		MockServerSession conn = new MockServerSession();
		
		server.sessionAccept(conn, hitId, assignmentId, workerId);
		
		assertEquals(conn.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_expfinished, ((Map<String, Object>) conn.lastData).get("status"));				
		
		// Try to submit the HIT and ensure it goes through
		server.updateCompletion();
		server.sessionSubmit(conn, "some random results");
				
		assertEquals(conn.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_completed, ((Map<String, Object>) conn.lastData).get("status"));
		assertEquals(1, server.completedHITs);
		
		// Make sure that a double-submit doesn't count twice
		server.updateCompletion();
		server.sessionSubmit(conn, "some random results");
		
		assertEquals(conn.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_completed, ((Map<String, Object>) conn.lastData).get("status") );
		assertEquals(1, server.completedHITs);
	}
	
	/*
	 * Test a reconnect after disconnecting
	 */
	@Test
	public void testReconnect() {
		String hitId = "testHITId";				
		String workerId = "testWorkerId";		
		String assignmentId = "testAssignmentId";
		
		Quiz passedQuiz = new Quiz();		
		passedQuiz.setNumCorrect(1);
		passedQuiz.setNumTotal(1);
		passedQuiz.setScore(1d);
		
		tracker.saveQuizResults(hitId, workerId, passedQuiz);
		
		MockServerSession conn = new MockServerSession();
		
		server.sessionAccept(conn, hitId, assignmentId, workerId);
		
		assertEquals(conn.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_connectlobby, ((Map<String, Object>) conn.lastData).get("status"));
	
		Session record = tracker.getStoredSessionInfo(hitId);
		assertEquals(record.getHitId(), hitId);
		assertEquals(record.getAssignmentId(), assignmentId);
		assertEquals(record.getWorkerId(), workerId);
		
		server.sessionDisconnect(conn);
		
		// Check workerId cleared
		assertEquals(record.getWorkerId(), null);
		
		MockServerSession conn2 = new MockServerSession();
		// Accept again
		server.sessionAccept(conn2, hitId, assignmentId, workerId);
		
		assertEquals(conn2.lastChannel, USER_CHANNEL);
		assertEquals(Codec.status_connectlobby, ((Map<String, Object>) conn2.lastData).get("status"));
		
		// Check workerId reassigned
		assertEquals(record.getWorkerId(), workerId);
	}
}
