package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.config.DataModule;
import edu.harvard.econcs.turkserver.config.DatabaseType;
import edu.harvard.econcs.turkserver.config.ExperimentType;
import edu.harvard.econcs.turkserver.config.HITCreation;
import edu.harvard.econcs.turkserver.config.LoggingType;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.config.TestServerModule;

/**
 * Test that the server starts and shuts down correctly
 * @author mao
 *
 */
public class ServerLifeCycleTest {

	static int clients = 0;
	static int groupSize = 5;
	
	TurkServer ts;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if( ts != null )
			ts.orderlyShutdown();
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

	@Test
	public void testShutdown() throws InterruptedException {
		DataModule dm = new DataModule();

		Configuration conf = dm.getConfiguration();
		conf.addProperty(TSConfig.SERVER_HITGOAL, clients);						
		conf.addProperty(TSConfig.EXP_REPEAT_LIMIT, clients);
		
		ts = new TurkServer(dm);
		
		ts.runExperiment(
				new GroupModule(),
				ExperimentType.GROUP_EXPERIMENTS,
				DatabaseType.TEMP_DATABASE,
				HITCreation.NO_HITS,
				LoggingType.SCREEN_LOGGING
				);
		
		ts.sessionServer.join();		

		Thread.sleep(1000);
		
		// Check that jetty is done
		assertTrue(ts.sessionServer.jettyCometD.server.isStopped());
		
		// Redundant call
		ts.awaitTermination();		
		// Check that gui is gone
		ts.disposeGUI();
		
		// TODO: shut down log4j/slf4j properly and check in here
		
		System.out.println(Thread.getAllStackTraces().keySet());
	}
}
