package edu.harvard.econcs.turkserver.api;

public interface Configurator {

	/**
	 * Sets up an experiment with a given treatment/input. 
	 * 
	 * TODO allow this to throw an exception
	 * 
	 * @param experiment
	 * @param inputData
	 */
	void configure(Object experiment, String inputData);

	/**
	 * Number of people required for each experiment
	 * @return
	 */
	int groupSize();
	
}
