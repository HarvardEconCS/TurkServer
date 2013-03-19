package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

public class TestConfigurator implements Configurator {
	
	final int groupSize;
	final int rounds;
	
	public TestConfigurator(int groupSize, int rounds) {
		this.groupSize = groupSize;
		this.rounds = rounds;
	}

	@Override
	public String configure(Object experiment, String expId, HITWorkerGroup group) {
		TestExperiment te = (TestExperiment) experiment;
				
		te.init(groupSize, rounds);
		
		return "test treatment";
	}

	@Override
	public int groupSize() {		
		return groupSize;
	}

}
