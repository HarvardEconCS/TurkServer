package edu.harvard.econcs.turkserver.config;

import com.google.inject.AbstractModule;

import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;
import edu.harvard.econcs.turkserver.server.mysql.MockDataTracker;
import edu.harvard.econcs.turkserver.server.mysql.MySQLDataTracker;

public class DatabaseType {

	public static final AbstractModule MYSQL_DATABASE = new AbstractModule() {
		@Override
		protected void configure() {
			bind(ExperimentDataTracker.class).to(MySQLDataTracker.class);
		}		
	};
	
	public static final AbstractModule TEMP_DATABASE = new AbstractModule() {
		@Override
		protected void configure() {
			bind(ExperimentDataTracker.class).to(MockDataTracker.class);
		}		
	};
}
