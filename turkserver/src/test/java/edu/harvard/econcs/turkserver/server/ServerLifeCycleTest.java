package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.server.TSBaseModule.TSTestModule;

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

	class GroupModule extends TSTestModule {
		@Override
		public void configure() {
			super.configure();
			
			setHITLimit(clients);
			
			bindGroupExperiments();
			bindExperimentClass(TestExperiment.class);				
			bindConfigurator(new TestConfigurator(groupSize, 0));
			bindString(TSConfig.EXP_SETID, "test");
		}
	}

	@Test
	public void testShutdown() throws InterruptedException {
		ss = TurkServer.testExperiment(new GroupModule());
		
		ss.join();
		
		assertTrue(ss.jettyCometD.server.isStopped());
	}
}
