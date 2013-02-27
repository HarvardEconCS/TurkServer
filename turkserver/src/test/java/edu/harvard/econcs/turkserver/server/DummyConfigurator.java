package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.api.Configurator;

public class DummyConfigurator implements Configurator {

	int groupSize;
	
	DummyConfigurator(int groupSize) {
		this.groupSize = groupSize;
	}
	
	@Override
	public void configure(Object experiment, String inputData) {

	}

	@Override
	public int groupSize() {		
		return groupSize;
	}

}
