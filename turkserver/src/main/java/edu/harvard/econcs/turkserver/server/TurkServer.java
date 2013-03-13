package edu.harvard.econcs.turkserver.server;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.swing.UIManager;

import net.andrewmao.misc.Utils;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.amazonaws.mturk.requester.QualificationRequirement;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.config.DataModule;
import edu.harvard.econcs.turkserver.config.ServerModule;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.mturk.TurkHITController;
import edu.harvard.econcs.turkserver.server.gui.ServerFrame;
import edu.harvard.econcs.turkserver.server.gui.TSTabbedPanel;
import edu.harvard.econcs.turkserver.server.mysql.MySQLDataTracker;

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
		
	final DataModule dataModule;

	// Parent injector that can still make DB connections and other stuff
	final Injector parentInjector;
	
	final ServerFrame gui;
	
	Injector childInjector;
	SessionServer sessionServer;
	
	public TurkServer(DataModule data) {	
		this.dataModule = data;
		
		parentInjector = Guice.createInjector(dataModule);

		gui = new ServerFrame(parentInjector.getInstance(TSTabbedPanel.class));
	}
	
	public TurkServer(Configuration conf) {
		this(new DataModule(conf));
	}
	
	public TurkServer(String confFile) throws FileNotFoundException, ConfigurationException {	
		this(new DataModule(confFile));
	}
	
	public void runExperiment(ServerModule serverModule, AbstractModule... otherModules) {
		if ( childInjector != null || sessionServer != null )
			throw new RuntimeException("TurkServer doesn't support concurrent experiments yet.");
		
		childInjector = parentInjector.createChildInjector(Lists.asList(serverModule, otherModules));
		
		Configuration conf = dataModule.getConfiguration();				
		
		String url = conf.getString(TSConfig.MTURK_HIT_EXTERNAL_URL, null);		
		if( url == null ) {
			System.out.println("URL not provided, finding public IP and port...");
			InetAddress publicAddr = Utils.getNetworkAddr();
			checkNotNull(publicAddr, "Couldn't find public a IP on this server");
			int port = conf.getInt(TSConfig.SERVER_HTTPPORT);			
			url = String.format("http://%s:%d/", publicAddr.getHostAddress(), port);
		}
		
		checkExperimentConfiguration(childInjector, conf);
		
		HITController thm = childInjector.getInstance(HITController.class);			
		
		// Post HITs if we are actually running on MTurk
		if( childInjector.getExistingBinding(Key.get(TurkHITController.class)) != null ) {
			// TODO this may not be in conf, but in injector (graph coloring stuff?)
			thm.setHITType(
					conf.getString(TSConfig.MTURK_HIT_TITLE),
					conf.getString(TSConfig.MTURK_HIT_DESCRIPTION),
					conf.getString(TSConfig.MTURK_HIT_KEYWORDS),
					conf.getDouble(TSConfig.MTURK_HIT_BASE_REWARD), 
					conf.getInt(TSConfig.MTURK_ASSIGNMENT_DURATION),
					conf.getInt(TSConfig.MTURK_AUTO_APPROVAL_DELAY),
					childInjector.getInstance(QualificationRequirement[].class));

			thm.setExternalParams(url, 
					conf.getInt(TSConfig.MTURK_HIT_FRAME_HEIGHT), 
					conf.getInt(TSConfig.MTURK_HIT_LIFETIME));		
		}
		
		// GUI is automatically created from parent injector now
		sessionServer = getSessionServerInstance(childInjector);
		
		sessionServer.start();		
		
		// post an adaptive number of HITs based on stuff
		int target = conf.getInt(TSConfig.SERVER_HITGOAL);
		int min = conf.getInt(TSConfig.HITS_MIN_OVERHEAD);
		int max = conf.getInt(TSConfig.HITS_MAX_OVERHEAD);
		double pct = conf.getDouble(TSConfig.HITS_OVERHEAD_PERCENT);
		int delay = conf.getInt(TSConfig.HITS_MIN_DELAY);
		
		thm.postBatchHITs(target, min, max, delay, pct);
	}
	
	public SessionServer getSessionServer() {
		return sessionServer;
	}

	private static SessionServer getSessionServerInstance(Injector injector) {
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

	/*
	 * Last check of sanity before we launch a server
	 */
	private static void checkExperimentConfiguration(Injector injector, Configuration conf) {
		boolean debugMode = conf.getBoolean(TSConfig.SERVER_DEBUGMODE);
		
		// Check MySQL configuration if using
		if( injector.getBinding(MySQLDataTracker.class) != null ) {
			MysqlConnectionPoolDataSource ds = injector.getInstance(MysqlConnectionPoolDataSource.class);
			
			try( Connection conn = ds.getConnection() ) {
				PreparedStatement sql = conn.prepareStatement("SHOW DATABASES");
				sql.execute();
			}
			catch( SQLException e ) {
				throw new RuntimeException("Database connection failed. Please check your settings.", e);
			}
		}
		
		// TODO check AWS config if using. Also could be a good chance to check for cash
		
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
		
		// Check for Turk settings if real HITs will be created
		if( injector.getExistingBinding(Key.get(TurkHITController.class)) != null ) {
			checkNotNull(conf.getDouble(TSConfig.MTURK_HIT_BASE_REWARD, null),
					"reward not specified");			
			
			checkNotNull(conf.getInteger(TSConfig.MTURK_HIT_FRAME_HEIGHT, null),
					"frame height not set");
			checkNotNull(conf.getInteger(TSConfig.MTURK_HIT_LIFETIME, null),
					"hit lifetime not set");
	
			checkNotNull( conf.getInteger(TSConfig.SERVER_HITGOAL, null), 
					"goal amount not specified");
			
			checkNotNull( conf.getDouble(TSConfig.HITS_OVERHEAD_PERCENT, null), 
					"HIT overhead percentage not specified ");
			checkNotNull( conf.getInteger(TSConfig.HITS_MIN_DELAY, null), 
					"delay not specified" );
			checkNotNull( conf.getInteger(TSConfig.HITS_MIN_OVERHEAD, null),
					"HIT min overhead not specified");
			checkNotNull( conf.getInteger(TSConfig.HITS_MAX_OVERHEAD, null),
					"HIT max overhead not specified");
		
			if( !debugMode ) { // Ignore these settings for local test
				checkNotNull(injector.getBinding(Key.get(QualificationRequirement[].class)),
						"No qualifications set!");
			}
		}						
		
		// Check that experiment class is proper
		Class<?> expClass = injector.getInstance(Key.get(new TypeLiteral<Class<?>>() {}, Names.named(TSConfig.EXP_CLASS)));
		checkNotNull(expClass, "experiment class not specified");
				
		EventAnnotationManager.testCallbacks(expClass);	
	}
	
	/**
	 * shuts down the experiment,
	 * waits for threads to finish,
	 * then disposes the GUI
	 */
	public void orderlyShutdown() {
		stopExperiment();
		awaitTermination();
		disposeGUI();
	}

	public void stopExperiment() {
		if( sessionServer == null ) return;
		
		sessionServer.shutdown();
	}
	
	/**
	 * Wait for servers to stop, then destroys the GUI.
	 */
	public void awaitTermination() {		
		if( sessionServer == null ) return;
		
		try {
			sessionServer.join();
		} catch (InterruptedException e) {}

		sessionServer = null;				
	}
	
	public void disposeGUI() {
		gui.dispose();
		
		// VM should exit here?
	}

	@Override
	protected void finalize() {
		// Was causing mysterious window closings
		// gui.dispose();
	}

	public static void main(String[] args) throws Exception {
		DataModule dm;
		
		if( args.length > 0 ) {
			String file = args[0];
			System.out.println("Trying to start TurkServer with " + file);
			dm = new DataModule(file);
		}
		else {
			System.out.println("Starting TurkServer with default settings (can't do much)");
			dm = new DataModule();
		}
		
		TurkServer ts = new TurkServer(dm);
		
		ts.awaitTermination();
	}
}
