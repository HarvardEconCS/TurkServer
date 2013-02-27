package edu.harvard.econcs.turkserver.config;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import edu.harvard.econcs.turkserver.server.GroupServer;
import edu.harvard.econcs.turkserver.server.SimpleExperimentServer;

public class ExperimentType {

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
	
}
