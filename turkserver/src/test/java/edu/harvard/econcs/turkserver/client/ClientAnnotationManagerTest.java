package edu.harvard.econcs.turkserver.client;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.api.BroadcastMessage;
import edu.harvard.econcs.turkserver.api.ClientController;
import edu.harvard.econcs.turkserver.api.ClientError;
import edu.harvard.econcs.turkserver.api.ExperimentClient;
import edu.harvard.econcs.turkserver.api.ServiceMessage;
import edu.harvard.econcs.turkserver.api.StartExperiment;
import edu.harvard.econcs.turkserver.api.StartRound;
import edu.harvard.econcs.turkserver.api.TimeLimit;

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

	@ExperimentClient
	static class TestClient {
		volatile String lastCall = null;		
		ClientController cont;
		
		public TestClient(ClientController cont) {
			this.cont = cont;			
		}
		
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
		
		@ClientError
		void clientError(String err) {
			lastCall = "clientError";
		}
		
		@BroadcastMessage
		void broadcast(Map<String, Object> msg) {
			lastCall = "broadcast";			
		}
		
		@ServiceMessage
		void service(Map<String, Object> msg) {
			lastCall = "service";
		}
	}
}
