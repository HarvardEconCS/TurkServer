package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

public class DummyConfigurator implements Configurator {

	int groupSize;
	
	DummyConfigurator(int groupSize) {
		this.groupSize = groupSize;
	}
	
	@Override
	public String configure(Object experiment, String expId, HITWorkerGroup group) {
		return "dummy-treatment";
	}

	@Override
	public int groupSize() {		
		return groupSize;
	}

}
