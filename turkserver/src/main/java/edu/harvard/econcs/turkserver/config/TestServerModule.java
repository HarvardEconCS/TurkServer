package edu.harvard.econcs.turkserver.config;

import java.util.List;
import org.eclipse.jetty.util.resource.Resource;

import com.amazonaws.mturk.requester.QualificationRequirement;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;

import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.server.QuizFactory;
import edu.harvard.econcs.turkserver.server.QuizPolicy;

public class TestServerModule extends ServerModule {
	
	@Override
	protected void configure() {
		super.configure();
		
		// Provide some default mturk settings for testing		
		
		bind(QuizFactory.class).toProvider(Providers.<QuizFactory>of(null));
		bind(QuizPolicy.class).toProvider(Providers.<QuizPolicy>of(null));			
		
		bindResources(new Resource[] {});
		bind(QualificationRequirement[].class).toProvider(Providers.<QualificationRequirement[]>of(null));				
		
		// TODO replace this with actual list of past experiments
		bind(new TypeLiteral<List<Experiment>>() {}).toProvider(Providers.of((List<Experiment>) null));						
	}		
}