package edu.harvard.econcs.turkserver.config;

import com.google.inject.AbstractModule;

import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.logging.ExperimentLogImpl;
import edu.harvard.econcs.turkserver.logging.FakeExperimentLog;
import edu.harvard.econcs.turkserver.logging.LogController;

public class LoggingType {

	public static final AbstractModule PERSIST_LOGGING = new AbstractModule() {
		@Override
		protected void configure() {
			bind(ExperimentLog.class).to(ExperimentLogImpl.class);
			bind(LogController.class).to(ExperimentLogImpl.class);	
		}		
	};
	
	/**
	 * Log experiment output to the screen; no output is saved.
	 */
	public static final AbstractModule SCREEN_LOGGING = new AbstractModule() {
		@Override
		protected void configure() {
			bind(ExperimentLog.class).to(FakeExperimentLog.class);
			bind(LogController.class).to(FakeExperimentLog.class);	
		}		
	};
}
