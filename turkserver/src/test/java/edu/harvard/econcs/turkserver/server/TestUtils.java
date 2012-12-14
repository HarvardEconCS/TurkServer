package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.HITWorker;

public class TestUtils {

	public static FakeHITWorkerGroup getFakeGroup(int groupSize, Class<?> clientClass) throws Exception {		
		FakeHITWorkerGroup fakeGroup 
			= new FakeHITWorkerGroup();
		
		for(int i = 1; i <= groupSize; i++ ) {									
			String hitId = "HIT " + i;
			String workerId = "Worker " + i;
			String assignmentId = "Assignment " + i;
			String username = "Username " + i;
			
			FakeHITWorker fake = FakeHITWorker.getNew(hitId, assignmentId, workerId, username, clientClass);
			
			fakeGroup.addWorker(fake);
		}
		
		return fakeGroup;
	}
	
	public static FakeExperimentController getFakeController(FakeHITWorkerGroup fakeGroup) {
		FakeExperimentController fakeCont = new FakeExperimentController(fakeGroup);
		
		for(HITWorker fake : fakeGroup.getHITWorkers() )
			((FakeHITWorker) fake).expCont = fakeCont;
		
		return fakeCont;
	}
	
	public static ExperimentLog getFakeLog() {
		return new FakeExperimentLog();
	}
	
}
