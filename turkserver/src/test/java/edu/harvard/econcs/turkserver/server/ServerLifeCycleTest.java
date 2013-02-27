package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

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
	
	SessionServer ss;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if( ss != null )
			ss.join();
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
		dm.setHITLimit(clients);
		
		ss = TurkServer.testExperiment(
				dm,
				ExperimentType.GROUP_EXPERIMENTS,
				DatabaseType.TEMP_DATABASE,
				HITCreation.NO_HITS,
				LoggingType.SCREEN_LOGGING,
				new GroupModule());
		
		ss.join();
		
		assertTrue(ss.jettyCometD.server.isStopped());
	}
}
