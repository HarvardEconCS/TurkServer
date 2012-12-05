package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.api.*;

public class EventAnnotationManagerTest {

	EventAnnotationManager m;
	
	@Before
	public void setUp() throws Exception {		
		m = new EventAnnotationManager();		
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void test() {
		TestExperiment e = new TestExperiment();		
		m.processExperiment("test", e);
		
		/*
		 * Test all callbacks
		 */
		m.triggerStart("test");
		assertEquals("startExp", e.lastCall);
		
		m.triggerRound("test", 0);
		assertEquals("startRound", e.lastCall);
		
		m.triggerTimelimit("test");
		assertEquals("timeLimit", e.lastCall);
		
		m.triggerWorkerConnect("test", null);
		assertEquals("connect", e.lastCall);
		
		m.triggerWorkerDisconnect("test", null);
		assertEquals("disconnect", e.lastCall);
		
		boolean result = m.deliverBroadcastMsg("test", null, null);
		assertEquals("broadcast", e.lastCall);
		assertEquals(true, result);
		
		m.deliverServiceMsg("test", null, null);
		assertEquals("service", e.lastCall);
		
		TestExperiment e2 = new TestExperiment();
		m.processExperiment("test2", e2);
		
		/*
		 * Should only have one class but two beans
		 */
		assertEquals(2, m.beans.size());
		assertEquals(2, m.beanClasses.size());
		assertEquals(1, m.beanClasses.keySet().size());
		
		assertEquals(1, m.starts.size());
		assertEquals(1, m.rounds.size());
		assertEquals(1, m.timeouts.size());
		assertEquals(1, m.connects.size());
		assertEquals(1, m.disconnects.size());
		assertEquals(1, m.broadcasts.size());
		assertEquals(1, m.services.size());
		
		m.deprocessExperiment("test2");
		
		/*
		 * Should only have one mapping but still the class
		 */
		assertEquals(1, m.beans.size());
		assertEquals(1, m.beanClasses.size());
		assertEquals(1, m.beanClasses.keySet().size());
		
		assertEquals(1, m.starts.size());
		assertEquals(1, m.rounds.size());
		assertEquals(1, m.timeouts.size());
		assertEquals(1, m.connects.size());
		assertEquals(1, m.disconnects.size());
		assertEquals(1, m.broadcasts.size());
		assertEquals(1, m.services.size());
		
		m.deprocessExperiment("test");						
		
		/*
		 * Should be all empty now
		 */
		assertEquals(0, m.beans.size());
		assertEquals(0, m.beanClasses.size());
		assertEquals(0, m.beanClasses.keySet().size());
		
		assertEquals(0, m.starts.size());
		assertEquals(0, m.rounds.size());
		assertEquals(0, m.timeouts.size());
		assertEquals(0, m.connects.size());
		assertEquals(0, m.disconnects.size());
		assertEquals(0, m.broadcasts.size());
		assertEquals(0, m.services.size());
	}

	@Experiment
	static class TestExperiment {
		volatile String lastCall = null;
		
		@StartExperiment
		void startExp() {
			lastCall = "startExp";
		}
		
		@StartRound
		void startRound(int n) {
			lastCall = "startRound";
		}
		
		@TimeLimit
		void timeLimit() {
			lastCall = "timeLimit";
		}
		
		@WorkerConnect
		void connect(HITWorker worker) {
			lastCall = "connect";
		}
		
		@WorkerDisconnect
		void disconnect(HITWorker worker) {
			lastCall = "disconnect";
		}
		
		@BroadcastMessage
		boolean broadcast(HITWorker worker, Map<String, Object> msg) {
			lastCall = "broadcast";
			return true;
		}
		
		@ServiceMessage
		void service(HITWorker worker, Map<String, Object> msg) {
			lastCall = "service";
		}
	}
}
