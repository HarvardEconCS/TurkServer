package edu.harvard.econcs.turkserver.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.eclipse.jetty.util.resource.Resource;

import com.amazonaws.mturk.util.ClientConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import edu.harvard.econcs.turkserver.api.*;
import edu.harvard.econcs.turkserver.logging.ExperimentLogImpl;
import edu.harvard.econcs.turkserver.logging.FakeExperimentLog;
import edu.harvard.econcs.turkserver.logging.LogController;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.mturk.TurkHITController;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.server.Assigner;
import edu.harvard.econcs.turkserver.server.EventAnnotationManager;
import edu.harvard.econcs.turkserver.server.Experiments;
import edu.harvard.econcs.turkserver.server.GroupServer;
import edu.harvard.econcs.turkserver.server.JettyCometD;
import edu.harvard.econcs.turkserver.server.Lobby;
import edu.harvard.econcs.turkserver.server.QuizFactory;
import edu.harvard.econcs.turkserver.server.QuizPolicy;
import edu.harvard.econcs.turkserver.server.ReadyStateLobby;
import edu.harvard.econcs.turkserver.server.SimpleExperimentServer;
import edu.harvard.econcs.turkserver.server.WorkerAuthenticator;
import edu.harvard.econcs.turkserver.server.gui.TSTabbedPanel;
import edu.harvard.econcs.turkserver.server.mturk.FakeHITController;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDummyTracker;
import edu.harvard.econcs.turkserver.server.mysql.MySQLDataTracker;
import edu.harvard.econcs.turkserver.util.RoundRobinAssigner;

public abstract class TSBaseModule extends AbstractModule {	
	
	protected Configuration conf;	
	
	protected TSBaseModule(String path) throws FileNotFoundException, ConfigurationException {				
		File confFile = new File(TSBaseModule.class.getClassLoader().getResource(path).getFile());
		if( !confFile.exists() ) throw new FileNotFoundException("configuration doesn't exist!");
		conf = TSConfig.getCustom(confFile);
		
		System.out.println("Loaded custom config file " + confFile);
	}
	
	protected TSBaseModule() {
		conf = TSConfig.getDefault();
	}
	
	public Configuration getConfiguration() {		
		return conf;
	}

	@Override
	protected void configure() {
		// Prevent unwanted initializations
		// binder().requireExplicitBindings();
		
		bind(Configuration.class).toInstance(conf);
		
		// Things will be JIT bound anyway, but required for explicit bindings		
		bind(EventAnnotationManager.class);
		bind(Experiments.class);
		bind(JettyCometD.class);
		bind(WorkerAuthenticator.class);
				
		bind(Assigner.class).to(RoundRobinAssigner.class);
		// bind(ExperimentLog.class).to(LogController.class);
		bind(Lobby.class).to(ReadyStateLobby.class);			
		
		bind(MysqlConnectionPoolDataSource.class).toProvider(new MysqlCPDSProvider()).asEagerSingleton();
		
		// GUI stuff
		bind(TSTabbedPanel.class).toInstance(new TSTabbedPanel());
	}
	
	protected void bindBoolean(String setting, boolean value) {
		bind(boolean.class).annotatedWith(Names.named(setting)).toInstance(value);
	}
	
	protected void bindInt(String setting, int value) {
		bind(int.class).annotatedWith(Names.named(setting)).toInstance(value);
	}

	protected void bindLong(String setting, long value) {
		bind(long.class).annotatedWith(Names.named(setting)).toInstance(value);
	}
	
	protected void bindDouble(String setting, double value) {
		bind(double.class).annotatedWith(Names.named(setting)).toInstance(value);		
	}

	protected void bindString(String setting, String value) {
		bind(String.class).annotatedWith(Names.named(setting)).toInstance(value);			
	}
	
	protected void bindExperimentClass(Class<?> beanClass) {
		// Explicit binding for experiment class
		// bind(beanClass);
		
		bind(new TypeLiteral<Class<?>>() {}).
		annotatedWith(Names.named(TSConfig.EXP_CLASS)).toInstance(beanClass);
	}
	
	protected void bindConfigurator(Configurator conf) {
		bind(Configurator.class).annotatedWith(Names.named(TSConfig.EXP_CONFIGURATOR)).toInstance(conf);
	}
	
	protected void bindAWSConfig(String accessKeyID, String secretAccessKey, boolean sandbox) {
		ClientConfig config = new ClientConfig();
		
		config.setAccessKeyId(accessKeyID);
		config.setSecretAccessKey(secretAccessKey);				
		config.setServiceURL(sandbox ? ClientConfig.SANDBOX_SERVICE_URL : ClientConfig.PRODUCTION_SERVICE_URL);
		
		bind(ClientConfig.class).toInstance(config);
	}
	
	protected void bindSingleExperiments() {
		bind(SimpleExperimentServer.class).in(Scopes.SINGLETON);
	}
	
	protected void bindGroupExperiments() {
		bind(GroupServer.class).in(Scopes.SINGLETON);
	}
	
	protected void bindTestClasses() {		
		bind(LogController.class).to(FakeExperimentLog.class);
		bind(HITController.class).to(FakeHITController.class);
		bind(ExperimentDataTracker.class).to(ExperimentDummyTracker.class);
	}
	
	protected void bindRealClasses() {
		bind(LogController.class).to(ExperimentLogImpl.class);
		bind(HITController.class).to(TurkHITController.class);
		bind(ExperimentDataTracker.class).to(MySQLDataTracker.class);
	}
	
	public class MysqlCPDSProvider implements Provider<MysqlConnectionPoolDataSource> {
		@Override
		public MysqlConnectionPoolDataSource get() {
			return TSConfig.getMysqlCPDS(conf);			
		}	
	}

	public static class TSTestModule extends TSBaseModule {
		public TSTestModule(String path) throws FileNotFoundException, ConfigurationException {
			super(path);			
		}
		
		public TSTestModule() {
			super();
		}

		protected void setHITLimit(int some_goal) {
			conf.addProperty(TSConfig.SERVER_HITGOAL, some_goal);						
			conf.addProperty(TSConfig.EXP_REPEAT_LIMIT, some_goal);
		}
		
		@Override
		protected void configure() {
			super.configure();
						
			bindTestClasses();
			
			bind(QuizFactory.class).toProvider(Providers.of((QuizFactory) null));
			bind(QuizPolicy.class).toProvider(Providers.of((QuizPolicy) null));			
			
			bind(Resource[].class).annotatedWith(Names.named(TSConfig.SERVER_RESOURCES)).toInstance(new Resource[] {});
			
			bind(new TypeLiteral<List<String>>() {})
			.annotatedWith(Names.named(TSConfig.EXP_SPECIAL_WORKERS)).toInstance(new LinkedList<String>());
			
			bind(new TypeLiteral<Set<String>>() {})
			.annotatedWith(Names.named(TSConfig.EXP_INPUT_LIST)).toInstance(Collections.singleton("test-treatment"));
			
			// TODO replace this with actual list of past experiments
			bind(new TypeLiteral<List<Experiment>>() {}).toProvider(Providers.of((List<Experiment>) null));						
		}		
	}
}