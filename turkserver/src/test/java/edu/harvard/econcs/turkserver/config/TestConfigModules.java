package edu.harvard.econcs.turkserver.config;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.logging.FakeExperimentLog;
import edu.harvard.econcs.turkserver.logging.LogController;
import edu.harvard.econcs.turkserver.mturk.FakeHITController;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;
import edu.harvard.econcs.turkserver.server.mysql.MockDataTracker;

public class TestConfigModules {

	public static final AbstractModule TEMP_DATABASE = new AbstractModule() {
		@Override
		protected void configure() {
			bind(ExperimentDataTracker.class).to(MockDataTracker.class);
		}		
	};
	/**
	 * Doesn't create any HITs on MTurk. You go to the link yourself.
	 */
	public static final AbstractModule NO_HITS = new AbstractModule() {
		@Override
		protected void configure() {			
			bind(HITController.class).to(FakeHITController.class).in(Scopes.SINGLETON);
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
