package edu.harvard.econcs.turkserver.server;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Good factories should cache an experiment to minimize server lag,
 * using the run() method
 * 
 * @author mao
 *
 * @param <T>
 */
@Deprecated
public abstract class ExpServerFactory<T> implements Runnable {
	
	/**
	 * Gets a new experiment.   
	 * 
	 * @param host
	 * @param clients
	 * @return
	 * @throws ExperimentFactoryException
	 */
	public abstract T getNewExperiment(GroupServer host, ConcurrentHashMap<String, Boolean> clients)
	throws ExperimentFactoryException;
		
	public abstract int getExperimentSize();

	/**
	 * Init method which can register serializers, etc
	 * @param host
	 */
	public abstract void doInit(GroupServer host);
	
	/**
	 * Default run method, which does nothing.
	 */
	public void run() {
		
	}
	
	public static class ExperimentFactoryException extends Exception {
		private static final long serialVersionUID = -5765011132751659410L;		
	}

}
