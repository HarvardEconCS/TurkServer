package edu.harvard.econcs.turkserver.config;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.logging.ExperimentLogImpl;
import edu.harvard.econcs.turkserver.logging.LogController;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.mturk.TurkHITController;
import edu.harvard.econcs.turkserver.server.GroupServer;
import edu.harvard.econcs.turkserver.server.SimpleExperimentServer;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;
import edu.harvard.econcs.turkserver.server.mysql.MySQLDataTracker;

public class ConfigModules {

	public static final AbstractModule SINGLE_EXPERIMENTS = new AbstractModule() {
		@Override
		protected void configure() {
			bind(SimpleExperimentServer.class).in(Scopes.SINGLETON);
		}		
	};

	public static final AbstractModule GROUP_EXPERIMENTS = new AbstractModule() {
		@Override
		protected void configure() {
			bind(GroupServer.class).in(Scopes.SINGLETON);
		}		
	};

	public static final AbstractModule MYSQL_DATABASE = new AbstractModule() {
		@Override
		protected void configure() {
			bind(ExperimentDataTracker.class).to(MySQLDataTracker.class);
		}		
	};
	
	public static final AbstractModule PERSIST_LOGGING = new AbstractModule() {
		@Override
		protected void configure() {
			bind(ExperimentLog.class).to(ExperimentLogImpl.class);
			bind(LogController.class).to(ExperimentLogImpl.class);	
		}		
	};
	
	public static final AbstractModule CREATE_HITS = new AbstractModule() {
		@Override
		protected void configure() {									
			bind(HITController.class).to(TurkHITController.class).in(Scopes.SINGLETON);
		}		
	};

}
