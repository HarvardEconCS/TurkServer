package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.api.ExperimentController;
import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

public class TestUtils {

	public static <C extends FakeHITWorker> FakeHITWorkerGroup<C> getFakeGroup(int groupSize, Class<C> workerClass) 
			throws InstantiationException, IllegalAccessException {
		
		FakeHITWorkerGroup<C> fakeGroup 
			= new FakeHITWorkerGroup<C>();
		
		for(int i = 1; i <= groupSize; i++ ) {
			C fake = workerClass.newInstance();
			
			fake.hitId = "HIT " + i;
			fake.workerId = "Worker " + i;
			fake.assignmentId = "Assignment " + i;
			fake.username = "Username " + i;
			
			fakeGroup.addWorker(fake);
		}
		
		return fakeGroup;
	}
	
	public static FakeExperimentController getFakeController(HITWorkerGroup fakeGroup) {
		return new FakeExperimentController(fakeGroup);
	}
	
	public static ExperimentLog getFakeLog() {
		return new FakeExperimentLog();
	}
	
}
