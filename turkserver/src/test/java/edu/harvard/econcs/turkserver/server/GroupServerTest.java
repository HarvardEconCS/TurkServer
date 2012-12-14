package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.server.TSBaseModule.TSTestModule;

public class GroupServerTest {

	static int groupSize = 5;
	
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
			
			bindGroupExperiments();
			bindExperimentClass(TestExperiment.class);				
			bindConfigurator(new TestConfigurator(groupSize));
			bindString(TSConfig.EXP_SETID, "test");
		}
	}
	
	@Test
	public void test() throws InterruptedException {
		TurkServer.testExperiment(new GroupModule());
		
		Thread.sleep(10000);
	}

}
