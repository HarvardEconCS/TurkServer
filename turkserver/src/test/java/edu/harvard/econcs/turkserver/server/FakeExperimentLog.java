package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.api.ExperimentLog;

public class FakeExperimentLog implements ExperimentLog {	
	
	@Override
	public long print(String msg) {		
		System.out.println(msg);
		return System.currentTimeMillis();
	}

	@Override
	public long printf(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
		return System.currentTimeMillis();
	}

}
