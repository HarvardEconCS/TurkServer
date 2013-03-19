package edu.harvard.econcs.turkserver.api;

public interface Configurator {

	/**
	 * Sets up an experiment with a given treatment/input. 
	 * 
	 * TODO allow this to throw an exception
	 * 
	 * @param experiment
	 * @param expId
	 * @param group
	 * @return the treatment id used for the experiment
	 */
	String configure(Object experiment, String expId, HITWorkerGroup group);

	/**
	 * Number of people required for each experiment
	 * @return
	 */
	int groupSize();
	
}
