package edu.harvard.econcs.turkserver.client;

import static org.junit.Assert.*;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ClientAnnotationManagerTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		// Test annotation checking
		ClientAnnotationManager<TestClient> m = 
				new ClientAnnotationManager<TestClient>(null, TestClient.class);		
		TestClient e = m.clientBean;
		
		/*
		 * Test all callbacks
		 */
		m.triggerStartExperiment();
		assertEquals("startExp", e.lastCall);
		
		m.triggerStartRound(0);
		assertEquals("startRound", e.lastCall);
		
		m.triggerTimeLimit();
		assertEquals("timeLimit", e.lastCall);

		m.triggerClientError("err");
		assertEquals("clientError", e.lastCall);		
		
		m.deliverBroadcast(null);
		assertEquals("broadcast", e.lastCall);
		
		m.deliverService(null);
		assertEquals("service", e.lastCall);
	}
}
