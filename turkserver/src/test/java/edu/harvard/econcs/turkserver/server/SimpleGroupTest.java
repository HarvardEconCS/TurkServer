package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.client.TestClient;
import edu.harvard.econcs.turkserver.config.DataModule;
import edu.harvard.econcs.turkserver.config.DatabaseType;
import edu.harvard.econcs.turkserver.config.ExperimentType;
import edu.harvard.econcs.turkserver.config.HITCreation;
import edu.harvard.econcs.turkserver.config.LoggingType;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.config.TestServerModule;

public class SimpleGroupTest {

	static int clients = 10;
	static int groupSize = 5;
	
	TurkServer ts;	
	ClientGenerator cg;
	
	@Before
	public void setUp() throws Exception {
		TestUtils.waitForPort(9876);
		TestUtils.waitForPort(9877);
		
		// Make sure the ports are clear
		Thread.sleep(500);
	}

	@After
	public void tearDown() throws Exception {
		if( cg != null ) {
			cg.disposeAllClients();
		}
		if( ts != null ) {
			ts.orderlyShutdown();
		}
	}

	class GroupModule extends TestServerModule {
		@Override
		public void configure() {
			super.configure();
						
			bindExperimentClass(TestExperiment.class);				
			bindConfigurator(new TestConfigurator(groupSize, 0));
			bindString(TSConfig.EXP_SETID, "test");
		}
	}
	
	public class TestListener implements ExperimentListener {
		volatile ExperimentControllerImpl lastStart;
		volatile ExperimentControllerImpl lastEnd;
		
		@Override
		public void experimentStarted(ExperimentControllerImpl exp) {
			lastStart = exp;	
		}
	
		@Override
		public void roundStarted(ExperimentControllerImpl exp) {
			// TODO Auto-generated method stub	
		}
	
		@Override
		public void experimentFinished(ExperimentControllerImpl exp) {
			lastEnd = exp;	
		}	
	}

	@Test(timeout=10000)
	public void test() throws Exception {
		DataModule dm = new DataModule();
		dm.setHITLimit(clients);
		
		ts = new TurkServer(dm);
		
		ts.runExperiment(
				new GroupModule(),
				ExperimentType.GROUP_EXPERIMENTS,
				DatabaseType.TEMP_DATABASE,
				HITCreation.NO_HITS,
				LoggingType.SCREEN_LOGGING
				);
		
		SessionServer ss = ts.sessionServer;
		
		TestListener tl = new TestListener();
		ss.experiments.registerListener(tl);					
		
		// Give server enough time to initialize
		Thread.sleep(500);
		
		cg = new ClientGenerator("http://localhost:9876/cometd/");
		
		// Add a group size
		List<LobbyClient<TestClient>> clients1 = new ArrayList<LobbyClient<TestClient>>(groupSize);
		for( int i = 0; i < groupSize; i++)
			clients1.add(cg.getClient(TestClient.class));
		
		Thread.sleep(500);		
		// Verify experiment started and everyone got the start message
		for( LobbyClient<TestClient> lc : clients1 )
			assertEquals("startExp", lc.getClientBean().lastCall );
		ExperimentControllerImpl ec1 = tl.lastStart;
		assertNotNull(ec1);
		TestExperiment exp1 = (TestExperiment) ss.experiments.manager.beans.get(ec1.getExpId());		
		assertNotNull(exp1);
		
		List<LobbyClient<TestClient>> clients2 = new ArrayList<LobbyClient<TestClient>>(groupSize);
		for( int i = 0; i < groupSize; i++)
			cg.getClient(TestClient.class);
		
		Thread.sleep(500);		
		for( LobbyClient<TestClient> lc : clients2 )
			assertEquals("startExp", lc.getClientBean().lastCall );
		ExperimentControllerImpl ec2 = tl.lastStart;
		assertNotNull(ec2);
		assertNotSame(ec1, ec2);
		
		TestExperiment exp2 = (TestExperiment) ss.experiments.manager.beans.get(ec2.getExpId());
		assertNotNull(exp2);
		assertNotSame(exp1, exp2);				
		
		// Try some broadcast messages		
		exp2.cont.sendExperimentBroadcast(
				ImmutableMap.of("msg", (Object) "test"));
		Thread.sleep(500);
		for( LobbyClient<TestClient> lc : clients2 )
			assertEquals("broadcast", lc.getClientBean().lastCall );
		
		exp1.cont.sendExperimentBroadcast(
				ImmutableMap.of("msg", (Object) "test"));
		Thread.sleep(500);
		for( LobbyClient<TestClient> lc : clients1 )
			assertEquals("broadcast", lc.getClientBean().lastCall );				
		
		// End the experiments
		exp2.cont.finishExperiment();
		Thread.sleep(500);
		for( LobbyClient<TestClient> lc : clients2 )
			assertEquals("finishExp", lc.getClientBean().lastCall );
		
		exp1.cont.finishExperiment();
		Thread.sleep(500);
		for( LobbyClient<TestClient> lc : clients1 )
			assertEquals("finishExp", lc.getClientBean().lastCall );		
				
		ts.awaitTermination();		

	}

}
