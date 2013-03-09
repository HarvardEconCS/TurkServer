package edu.harvard.econcs.turkserver.config;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.util.resource.Resource;

import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;

import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.server.QuizFactory;
import edu.harvard.econcs.turkserver.server.QuizPolicy;

public class TestServerModule extends ServerModule {
	
	@Override
	protected void configure() {
		super.configure();
		
		// Provide some default mturk settings for testing
		
		
		bind(QuizFactory.class).toProvider(Providers.of((QuizFactory) null));
		bind(QuizPolicy.class).toProvider(Providers.of((QuizPolicy) null));			
		
		bindResources(new Resource[] {});
		
		bind(new TypeLiteral<Set<String>>() {})
		.annotatedWith(Names.named(TSConfig.EXP_INPUT_LIST)).toInstance(Collections.singleton("test-treatment"));
		
		// TODO replace this with actual list of past experiments
		bind(new TypeLiteral<List<Experiment>>() {}).toProvider(Providers.of((List<Experiment>) null));						
	}		
}