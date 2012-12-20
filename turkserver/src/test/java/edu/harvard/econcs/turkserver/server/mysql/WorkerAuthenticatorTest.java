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
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.TSConfig;
import edu.harvard.econcs.turkserver.server.WorkerAuthenticator;
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
				conf,
				specialWorkers);
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void testHITValidEmpty() throws Exception {
		String hitId = "testHITId";
		workerAuth.checkHITValid(hitId, "testWorkerId");		
	}
	
	@Test
	public void testHITValidExisting() throws Exception {
		String hitId = "testHITId";
		tracker.saveHITId(hitId);
		workerAuth.checkHITValid(hitId, "testWorkerId");		
	}
	
	@Test(expected=SessionOverlapException.class)
	public void testHITValidOverlap() throws Exception {
		String hitId = "testHITId";
		
		Session s = new Session();		
		s.setHitId(hitId);
		s.setWorkerId("someOtherWorker");
		s.setInactivePercent(0.00);
		
		tracker.saveSession(s);
		
		workerAuth.checkHITValid(hitId, "thisWorker");				
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
		
		workerAuth.checkHITValid(hitId, workerId);				
	}
	
	@Test
	public void testLimitNone() {
		fail("not implemented");
	}
	
	@Test(expected=SimultaneousSessionsException.class)
	public void testLimitConcurrent() {
		fail("not implemented");
	}
	
	@Test(expected=TooManySessionsException.class)
	public void testLimitSets() {
		fail("not implemented");
	}

	@Test
	public void testAssignAccept() {
		fail("not implemented");
	}
	
	@Test(expected=SessionCompletedException.class)
	public void testAssignCompleted() {
		// TODO don't think we need two checks for this...first should be sufficient
		
		fail("not implemented");
	}
	
	@Test(expected=SessionOverlapException.class)
	public void testAssignOverlap() {
		fail("not implemented");
	}
	
}
