package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.cometd.MockServerSession;
import edu.harvard.econcs.turkserver.logging.FakeExperimentLog;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.mysql.MockDataTracker;
import edu.harvard.econcs.turkserver.util.RoundRobinAssigner;

public class ExperimentsTest {
	
	Experiments exps;
	Session record;
	
	@Before
	public void setUp() throws Exception {
		exps = new Experiments(
				TestExperiment.class,
				new DummyConfigurator(1),
				new RoundRobinAssigner(Collections.singleton("test"), null),
				new MockDataTracker(),
				new EventAnnotationManager()
				);
		
		record = new Session();
		record.setHitId("TestHITID");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testIntervals() throws InterruptedException {
		
		HITWorkerImpl worker = new HITWorkerImpl(new MockServerSession(), record);
		FakeExperimentLog log = new FakeExperimentLog();
		ExperimentControllerImpl cont = new ExperimentControllerImpl(log, worker, exps);
		
		TestExperiment bean = new TestExperiment(worker, log, cont);
		
		exps.startExperiment(worker, cont, bean);
		
		// Check that worker is mapped
		assertTrue(exps.currentExps.containsKey(worker));		
		Thread.sleep(5000);
		
		exps.scheduleFinishExperiment(cont);		
		Thread.sleep(500); // Allow for time to finish

		// check that scheduledIntervals is cleared
		assertTrue(exps.scheduledIntervals.size() == 0);		
		// Check that worker is unmapped
		assertFalse(exps.currentExps.containsKey(worker));		
		// check that there were approx 5 interval calls
		assertEquals(5, bean.intervalCalls, 1);		

	}

}
