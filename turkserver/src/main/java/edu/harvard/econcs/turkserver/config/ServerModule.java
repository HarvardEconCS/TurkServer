package edu.harvard.econcs.turkserver.config;

import org.eclipse.jetty.util.resource.Resource;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import edu.harvard.econcs.turkserver.api.*;
import edu.harvard.econcs.turkserver.server.Assigner;
import edu.harvard.econcs.turkserver.server.EventAnnotationManager;
import edu.harvard.econcs.turkserver.server.ExperimentControllerImpl;
import edu.harvard.econcs.turkserver.server.ExperimentScoped;
import edu.harvard.econcs.turkserver.server.Experiments;
import edu.harvard.econcs.turkserver.server.JettyCometD;
import edu.harvard.econcs.turkserver.server.Lobby;
import edu.harvard.econcs.turkserver.server.ReadyStateLobby;
import edu.harvard.econcs.turkserver.server.ThreadLocalScope;
import edu.harvard.econcs.turkserver.server.WorkerAuthenticator;
import edu.harvard.econcs.turkserver.server.gui.TSTabbedPanel;
import edu.harvard.econcs.turkserver.util.RoundRobinAssigner;

public abstract class ServerModule extends AbstractModule {	
	
	@Override
	protected void configure() {		
		// Prevent unwanted initializations
		// binder().requireExplicitBindings();

		// create thread-local scope for initializing experiments
		ThreadLocalScope scope = new ThreadLocalScope();
		bindScope(ExperimentScoped.class, scope);
		bind(ThreadLocalScope.class).toInstance(scope);
		
		// Things will be JIT bound anyway, but required for explicit bindings		
		bind(EventAnnotationManager.class);
		bind(Experiments.class);
		bind(JettyCometD.class);
		bind(WorkerAuthenticator.class);
				
		bind(Assigner.class).to(RoundRobinAssigner.class);
		bind(Lobby.class).to(ReadyStateLobby.class);
		
		// bind(ExperimentLog.class).to(LogController.class);		
		bind(ExperimentController.class).to(ExperimentControllerImpl.class);
		
		bind(HITWorker.class).toProvider(ThreadLocalScope.<HITWorker>seededKeyProvider()).in(ExperimentScoped.class);
		bind(HITWorkerGroup.class).toProvider(ThreadLocalScope.<HITWorkerGroup>seededKeyProvider()).in(ExperimentScoped.class);
				
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

	
	protected void bindResources(Resource[] rscs) {
		bind(Resource[].class).annotatedWith(Names.named(TSConfig.SERVER_RESOURCES)).toInstance(rscs);
	}

}