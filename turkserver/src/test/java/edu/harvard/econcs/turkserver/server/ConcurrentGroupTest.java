package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.client.TestClient;
import edu.harvard.econcs.turkserver.config.DataModule;
import edu.harvard.econcs.turkserver.config.DatabaseType;
import edu.harvard.econcs.turkserver.config.ExperimentType;
import edu.harvard.econcs.turkserver.config.HITCreation;
import edu.harvard.econcs.turkserver.config.LoggingType;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.config.TestServerModule;

public class ConcurrentGroupTest {
	
	static int clients = 50;
	static int groupSize = 5;	
	
	static int rounds = 5;
	static int delay = 1000;					
	
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
			bindConfigurator(new TestConfigurator(groupSize, rounds));
			bindString(TSConfig.EXP_SETID, "test");
		}
	}

	@Test(timeout=20000)
	public void test() throws Exception {
		DataModule dataModule = new DataModule();
		
		Configuration conf = dataModule.getConfiguration();
		conf.setProperty(TSConfig.SERVER_DEBUGMODE, true); // No waiting for hit submits
		conf.setProperty(TSConfig.SERVER_LOBBY_DEFAULT, true);
		conf.setProperty(TSConfig.SERVER_HITGOAL, clients);						
		conf.setProperty(TSConfig.EXP_REPEAT_LIMIT, clients);
		
		TurkServer ts = new TurkServer(dataModule);
		
		ts.runExperiment(
				new GroupModule(),
				DatabaseType.TEMP_DATABASE,
				ExperimentType.GROUP_EXPERIMENTS,
				HITCreation.NO_HITS,
				LoggingType.SCREEN_LOGGING				
				);
		
		SessionServer ss = ts.sessionServer;
		
		// Give server enough time to initialize
		Thread.sleep(500);
		
		cg = new ClientGenerator("http://localhost:9876/cometd/");
				
		LinkedList<TestClient> ll = Lists.newLinkedList();
		
		for( int i = 0; i < clients; i++) {
			LobbyClient<TestClient> lc = cg.getClient(TestClient.class);			
			
			TestClient cl = lc.getClientBean();
			cl.setMessage(String.valueOf(i), delay);
			ll.add(cl);
		}		
		
		/*
		 * TODO: 2 earliest experiments are not completing when unit test is run in a group! 
		 */
		
		// Wait for server to shut down
		ss.join();
		
		// Verify that every client finished correctly
		for( TestClient cl : ll )
			assertTrue(cl.finished);
		
		// Verify that every experiment is finished
		assertEquals(0, ss.experiments.manager.beans.size() );
		assertEquals(0, ss.experiments.currentExps.size());
		
	}

}
