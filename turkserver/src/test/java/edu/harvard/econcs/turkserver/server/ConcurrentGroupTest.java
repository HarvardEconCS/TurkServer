package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.client.TestClient;
import edu.harvard.econcs.turkserver.server.TSBaseModule.TSTestModule;

public class ConcurrentGroupTest {
	
	static int clients = 100;
	static int groupSize = 5;	
	
	static int rounds = 5;
	static int delay = 1000;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	class GroupModule extends TSTestModule {
		@Override
		public void configure() {
			super.configure();
			
			setHITLimit(clients);
			
			bindGroupExperiments();
			bindExperimentClass(TestExperiment.class);				
			bindConfigurator(new TestConfigurator(groupSize, rounds));
			bindString(TSConfig.EXP_SETID, "test");
		}
	}

	@Test(timeout=20000)
	public void test() throws Exception {
		SessionServer ss = TurkServer.testExperiment(new GroupModule());
		
		ClientGenerator cg = new ClientGenerator("http://localhost:9876/cometd/");
		
		LinkedList<TestClient> ll = Lists.newLinkedList();
		
		for( int i = 0; i < clients; i++) {
			LobbyClient<TestClient> lc = cg.getClient(TestClient.class);
			TestClient cl = lc.getClientBean();
			cl.setMessage(String.valueOf(i), delay);
			ll.add(cl);
		}
		
		// Wait for server to shut down
		ss.join();
		
		// Verify that every client finished correctly
		for( TestClient cl : ll )
			assertTrue(cl.finished);
	}

}
