package edu.harvard.econcs.turkserver.config;

import com.google.inject.AbstractModule;

import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.mturk.TurkHITController;
import edu.harvard.econcs.turkserver.server.mturk.FakeHITController;

public class HITCreation {

	/**
	 * Doesn't create any HITs on MTurk. You go to the link yourself.
	 */
	public static final AbstractModule NO_HITS = new AbstractModule() {
		@Override
		protected void configure() {			
			bind(HITController.class).to(FakeHITController.class);
		}		
	};
	
	public static final AbstractModule CREATE_HITS = new AbstractModule() {
		@Override
		protected void configure() {									
			bind(HITController.class).to(TurkHITController.class);
		}		
	};
}
