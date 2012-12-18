package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.api.Configurator;

public class TestConfigurator implements Configurator {
	
	final int groupSize;
	final int delay;
	
	public TestConfigurator(int groupSize, int delay) {
		this.groupSize = groupSize;
		this.delay = delay;
	}

	@Override
	public void configure(Object experiment, String inputData) {
		TestExperiment te = (TestExperiment) experiment;
				
		te.init(groupSize, delay);
	}

	@Override
	public int groupSize() {		
		return groupSize;
	}

}
