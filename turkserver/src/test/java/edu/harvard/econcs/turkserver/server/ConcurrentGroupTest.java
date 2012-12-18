package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.client.TestClient;
import edu.harvard.econcs.turkserver.server.TSBaseModule.TSTestModule;

public class ConcurrentGroupTest {
	
	static int clients = 100;
	static int groupSize = 5;	
	
	static int rounds = 10;
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

	@Test
	public void test() throws Exception {
		SessionServer ss = TurkServer.testExperiment(new GroupModule());
		
		ClientGenerator cg = new ClientGenerator("http://localhost:9876/cometd/");
		
		for( int i = 0; i < clients; i++) {
			LobbyClient<TestClient> lc = cg.getClient(TestClient.class);
			lc.getClientBean().setMessage(String.valueOf(i), delay);
		}
		
		ss.join();		
	}

}
