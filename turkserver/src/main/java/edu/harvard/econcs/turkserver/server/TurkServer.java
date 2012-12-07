package edu.harvard.econcs.turkserver.server;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.util.resource.Resource;

import com.amazonaws.mturk.util.ClientConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.server.mturk.FakeHITController;
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
	
	public static void testSimpleExperiment(
			final ClientConfig config,
			final Class<?> beanClass, 
			final Configurator configurator,
			final String setId) {
				
		Injector injector = Guice.createInjector(new TSTestModule() {
			@Override
			public void configure() {
				super.configure();
				
				bind(ClientConfig.class).toInstance(config);
				bind(SessionServer.class).to(SimpleExperimentServer.class).in(Scopes.SINGLETON);
				
				bind(new TypeLiteral<Class<?>>() {})
				.annotatedWith(Names.named(TSConfig.EXP_CLASS)).toInstance(beanClass);
				
				bind(Configurator.class)
				.annotatedWith(Names.named(TSConfig.EXP_INIT)).toInstance(configurator);
				
				bind(String.class)
				.annotatedWith(Names.named(TSConfig.EXP_SETID)).toInstance(setId);
			}
		});
		
		HITController thm = injector.getInstance(HITController.class);
		
		thm.setHITType("test", "test desc", "keyword1 keyword2", 
				1.00, 86400, 604800, null);
		
		thm.setExternalParams("http://localhost:9294/", 1500, 604800);
		
		SimpleExperimentServer ss = injector.getInstance(SimpleExperimentServer.class);
		
		new Thread(ss).start();		
		
		thm.postBatchHITs(1, 5000, 10);
	}
	
	public static void createSynchronousExperiment() {
		
	}
	
	static class TSBaseModule extends AbstractModule {
		@Override
		protected void configure() {			
			bind(int.class).annotatedWith(Names.named(TSConfig.CONCURRENCY_LIMIT)).toInstance(1);
			
			bind(ExperimentDataTracker.class).to(MySQLDataTracker.class);			
		}
	}
	
	static class TSTestModule extends AbstractModule {
		@Override
		protected void configure() {
			int some_goal = 10;
			
			// TODO maybe this should be named too			
			Configuration conf = TSConfig.getDefault();
			conf.addProperty(TSConfig.SERVER_HITGOAL, some_goal);
			
			bind(int.class)
			.annotatedWith(Names.named(TSConfig.SERVER_HTTPPORT)).toInstance(conf.getInt(TSConfig.SERVER_HTTPPORT));
			
			bind(int.class)
			.annotatedWith(Names.named(TSConfig.CONCURRENCY_LIMIT)).toInstance(1);			
			
			bind(int.class)
			.annotatedWith(Names.named(TSConfig.SET_REPEAT_LIMIT)).toInstance(some_goal);
			
			bind(HITController.class).to(FakeHITController.class);
			bind(ExperimentDataTracker.class).to(ExperimentDummyTracker.class);
			
			bind(QuizFactory.class).toProvider(Providers.of((QuizFactory) null));
			bind(QuizPolicy.class).toProvider(Providers.of((QuizPolicy) null));
			
			bind(Configuration.class).toInstance(conf);
			bind(Resource[].class).toInstance(new Resource[] {});
			
			bind(new TypeLiteral<List<String>>() {})
			.annotatedWith(Names.named(TSConfig.SPECIAL_WORKERS)).toInstance(new LinkedList<String>());
			
			bind(new TypeLiteral<Set<String>>() {})
			.annotatedWith(Names.named(TSConfig.INPUT_LIST)).toInstance(Collections.singleton("test-treatment"));
			
			// TODO replace this with actual list of past experiments
			bind(new TypeLiteral<List<Experiment>>() {}).toProvider(Providers.of((List<Experiment>) null));
						
		}
	}
}
