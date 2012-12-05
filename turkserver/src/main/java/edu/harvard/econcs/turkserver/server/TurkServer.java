package edu.harvard.econcs.turkserver.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import edu.harvard.econcs.turkserver.server.mysql.*;

/**
 * The main TurkServer class.
 * 
 * Contains static methods for starting experiments.
 * 
 * @author mao
 *
 */
public class TurkServer {
	
	public static void createSimpleExperiment() {		
		Injector injector = Guice.createInjector(new TSBaseModule());
		
		SimpleExperimentServer ss = injector.getInstance(SimpleExperimentServer.class);
		
		new Thread(ss).start();
	}
	
	public static void createSynchronousExperiment() {
		
	}
	
	static class TSBaseModule extends AbstractModule {	
		@Override
		protected void configure() {			
			
			bind(ExperimentDataTracker.class).to(MySQLDataTracker.class);
		}
	}
}
