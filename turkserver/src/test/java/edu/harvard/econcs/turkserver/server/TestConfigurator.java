package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.api.Configurator;

public class TestConfigurator implements Configurator {
	
	final int groupSize;
	final int rounds;
	
	public TestConfigurator(int groupSize, int rounds) {
		this.groupSize = groupSize;
		this.rounds = rounds;
	}

	@Override
	public void configure(Object experiment, String inputData) {
		TestExperiment te = (TestExperiment) experiment;
				
		te.init(groupSize, rounds);
	}

	@Override
	public int groupSize() {		
		return groupSize;
	}

}
