package edu.harvard.econcs.turkserver.server;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.swing.UIManager;

import org.apache.commons.configuration.Configuration;

import com.amazonaws.mturk.requester.QualificationRequirement;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.config.DataModule;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.server.gui.ServerFrame;
import edu.harvard.econcs.turkserver.server.gui.TSTabbedPanel;

/**
 * The main TurkServer class.
 * 
 * Contains static methods for starting experiments.
 * 
 * @author mao
 *
 */
public class TurkServer {
	
	static {
		// Set GUI LnF
		try {
			// com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel.class.getCanonicalName();			
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");			
		} catch (Exception e) {	e.printStackTrace(); }	
	}
	
	/*
	 * Last check of sanity before we launch a server
	 */
	private static void checkConfiguration(Injector injector, Configuration conf) {
		
		// Check bindings
		checkNotNull(injector.getBinding(Key.get(String.class, Names.named(TSConfig.EXP_SETID))),
				"set not specified");
		checkNotNull(injector.getBinding(Key.get(Configurator.class, Names.named(TSConfig.EXP_CONFIGURATOR))),
				"experiment configurator not specified");
		
		Binding<QuizFactory> qf = injector.getBinding(Key.get(QuizFactory.class));
		Binding<QuizPolicy> qp = injector.getBinding(Key.get(QuizPolicy.class));
		checkArgument((qf == null) == (qp == null), "Either both QuizFactory and QuizPolicy must be specified, or neither");
		
		// Check properties
		checkNotNull(conf.getProperty(TSConfig.CONCURRENCY_LIMIT), "concurrent limit not specified");
		checkNotNull(conf.getProperty(TSConfig.EXP_REPEAT_LIMIT), "set limit not specified");
		
		boolean debugMode = conf.getBoolean(TSConfig.SERVER_DEBUGMODE);
		
		if( !debugMode ) { // Ignore these settings for local test
			checkNotNull(conf.getDouble(TSConfig.MTURK_HIT_BASE_REWARD, null),
					"reward not specified");
			
			checkNotNull(injector.getBinding(Key.get(QualificationRequirement[].class)),
					"No qualifications set!");
			
			checkNotNull(conf.getInteger(TSConfig.MTURK_HIT_FRAME_HEIGHT, null),
					"frame height not set");
			checkNotNull(conf.getInteger(TSConfig.MTURK_HIT_LIFETIME, null),
					"hit lifetime not set");
			checkNotNull(conf.getString(TSConfig.MTURK_HIT_EXTERNAL_URL, null),
					"external url not set");
	
			// TODO update these when more flexible config is created 
			checkNotNull( conf.getInteger(TSConfig.HITS_INITIAL, null), 
					"initial HITs not specified ");
			checkNotNull( conf.getInteger(TSConfig.HITS_DELAY, null), 
					"delay not specified" );
			checkNotNull( conf.getInteger(TSConfig.SERVER_HITGOAL, null), 
					"goal amount not specified");
			checkNotNull( conf.getInteger(TSConfig.HITS_TOTAL, null),
					"total HITs not specified");
		}
		
	}

	private static SessionServer getSessionServer(Injector injector) {
		if( injector.getExistingBinding(new Key<SimpleExperimentServer>() {}) != null ) {
			return injector.getInstance(SimpleExperimentServer.class);
		}
		else if( injector.getExistingBinding(new Key<GroupServer>() {}) != null ) {
			return injector.getInstance(GroupServer.class);
		}
		else {
			throw new RuntimeException("No binding found for session server. " +
					"Try bindSingleExperiments() or bindGroupExperiments() in your module.");
		}		
	}

	public static SessionServer testExperiment(DataModule dataModule, AbstractModule... otherModules) {
		
		Injector injector = Guice.createInjector(Lists.asList(dataModule, otherModules));
		
		Configuration conf = dataModule.getConfiguration();				
		checkConfiguration(injector, conf);
		
		HITController thm = injector.getInstance(HITController.class);
		new ServerFrame(injector.getInstance(TSTabbedPanel.class));
		
		SessionServer ss = getSessionServer(injector);
		
		// TODO this may not be in conf, but in injector
		thm.setHITType(
				conf.getString(TSConfig.MTURK_HIT_TITLE),
				conf.getString(TSConfig.MTURK_HIT_DESCRIPTION),
				conf.getString(TSConfig.MTURK_HIT_KEYWORDS),
				1.00, 
				conf.getInt(TSConfig.MTURK_ASSIGNMENT_DURATION),
				conf.getInt(TSConfig.MTURK_AUTO_APPROVAL_DELAY),
				null);
		
		thm.setExternalParams("http://localhost:9294/", 1500, 604800);
		
		ss.start();		
		
		thm.postBatchHITs(1, 5000, 10);
		
		return ss;
	}

	public static SessionServer runExperiment(DataModule dataModule, AbstractModule... otherModules) {		
		return null;		
	}

	public static void main(String[] args) {
		// TODO start this from an injector, or it won't be able to launch experiments
		new ServerFrame(new TSTabbedPanel());
	}
}
