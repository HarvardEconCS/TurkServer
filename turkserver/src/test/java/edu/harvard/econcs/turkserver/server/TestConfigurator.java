package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.api.Configurator;

public class TestConfigurator implements Configurator {
	
	final int groupSize;
	
	public TestConfigurator(int groupSize) {
		this.groupSize = groupSize;
	}

	@Override
	public void configure(Object experiment, String inputData) {
		// TODO Auto-generated method stub

	}

	@Override
	public int groupSize() {		
		return groupSize;
	}

}
