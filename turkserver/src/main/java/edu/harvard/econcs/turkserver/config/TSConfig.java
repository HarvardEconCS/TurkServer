package edu.harvard.econcs.turkserver.config;

import java.io.File;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.amazonaws.mturk.util.ClientConfig;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

public class TSConfig {

	/* ****************************************************
	 * Default values
	 ******************************************************/
	
	public static final String TURKSERVER_CONFIG = "turkserver.properties";
	
	/**
	 * port that the server runs on. +1 is also used. 
	 */
	public static final String SERVER_HTTPPORT = "server.httpport";
	public static final String SERVER_DEBUGMODE = "server.debugmode";
	
	/**
	 * Maximum number of assigned, non-completed HITs that a worker can have (usually 1).
	 */
	public static final String CONCURRENCY_LIMIT = "session.concurrent.limit";	
	
	public static final String MTURK_ASSIGNMENT_DURATION = "mturk.assignmentDurationInSeconds";
	public static final String MTURK_AUTO_APPROVAL_DELAY = "mturk.autoApprovalDelayInSeconds";
	public static final String MTURK_HIT_LIFETIME = "mturk.lifetimeInSeconds";		
	
	/* ****************************************************
	 * Data dependencies
	 ******************************************************/
	
	public static final String AWS_ACCESSKEYID = "aws.accessKeyID";
	public static final String AWS_SECRETACCESSKEY = "aws.secretAccessKey";
	public static final String AWS_SANDBOX = "aws.sandbox";
	
	public static final String MYSQL_HOST = "mysql.host";
	public static final String MYSQL_DATABASE = "mysql.database";
	public static final String MYSQL_USER = "mysql.username";
	public static final String MYSQL_PASSWORD = "mysql.password";
	
	/* ****************************************************
	 * Other things that will need to be set
	 ******************************************************/
	
	public static final String EXP_CONFIGURATOR = "experiment.configurator";
	public static final String EXP_CLASS = "experiment.class";	
	public static final String EXP_SETID = "experiment.setid";	
	
	public static final String EXP_INPUT_LIST = "experiment.inputdata";	
	
	/**
	 * Maximum number of HITs a single worker can complete in this set
	 */
	public static final String EXP_REPEAT_LIMIT = "experiment.set.limit";		
	
	public static final String MTURK_HIT_TITLE = "mturk.hit.title";
	public static final String MTURK_HIT_DESCRIPTION = "mturk.hit.description";
	public static final String MTURK_HIT_KEYWORDS = "mturk.hit.keywords";	
	public static final String MTURK_HIT_BASE_REWARD = "mturk.hit.reward";
	public static final String MTURK_HIT_FRAME_HEIGHT = "mturk.hit.frameheight";		
	public static final String MTURK_HIT_EXTERNAL_URL = "mturk.hit.external.url";
	
	public static final String SERVER_RESOURCES = "server.resources";
	
	/**
	 * Number of HITs to be completed before server initiates shutdown
	 */
	public static final String SERVER_HITGOAL = "server.hitgoal";
	public static final String SERVER_LOBBY_DEFAULT = "server.lobby.default";
	public static final String SERVER_USERNAME = "server.usernames";			
	/**
	 * Minimum number of milliseconds between creating HITs
	 */
	public static final String HITS_MIN_DELAY = "server.hit.mindelay";
	/**
	 * Maximum amount of millis between creating HITs (keeps at top of list)
	 */
	public static final String HITS_MAX_DELAY = "server.hit.maxdelay";
	/**
	 * Overhead, percentage wise, of HITs that should be created (0 < x < 1)
	 */
	public static final String HITS_OVERHEAD_PERCENT = "server.hit.overheadpct";
	/**
	 * Minimum number of extra HITs that should be available
	 */
	public static final String HITS_MIN_OVERHEAD = "server.hit.minoverhead";
	/**
	 * Maximum number of extra HITs that should be created
	 */
	public static final String HITS_MAX_OVERHEAD = "server.hit.maxoverhead";	
	
	// Optional parameters
	public static final String EXP_SPECIAL_WORKERS = "experiment.special.workers";
	
	public static final String SERVER_EXTRA_SERVLETS = "server.extra.servlets";
	public static final String SERVER_CUSTOM_HANDLERS = "server.custom.handlers";
	
	public static Configuration getDefault() {
		Configuration conf = null;
		try {			
			conf = new PropertiesConfiguration(TURKSERVER_CONFIG);
			System.out.printf("Found and loaded %s\n", TURKSERVER_CONFIG);
		} catch (ConfigurationException e) {
			System.out.printf("Unable to load %s. Proceeding with default settings...\n", TURKSERVER_CONFIG);
			conf = new PropertiesConfiguration();
		}		
		
		// Sensible default values to fall back on		
		conf.setProperty(SERVER_HTTPPORT, 9876);
		conf.setProperty(CONCURRENCY_LIMIT, 1);
		
		conf.setProperty(MTURK_ASSIGNMENT_DURATION, 86400);
		conf.setProperty(MTURK_AUTO_APPROVAL_DELAY, 604800);
		conf.setProperty(MTURK_HIT_LIFETIME, 604800);
		
		// TODO: remove this
		conf.setProperty(SERVER_DEBUGMODE, false);
		
		conf.setProperty(SERVER_LOBBY_DEFAULT, true);
		conf.setProperty(SERVER_USERNAME, false);
		
		// Sensible defaults for creating HITs		
		conf.setProperty(HITS_MIN_DELAY, 1000);
		conf.setProperty(HITS_MAX_DELAY, 300 * 1000); // posts at most every 5 minutes
		conf.setProperty(HITS_OVERHEAD_PERCENT, 0.1);
		conf.setProperty(HITS_MIN_OVERHEAD, 10);
		conf.setProperty(HITS_MAX_OVERHEAD, 50);
		
		return conf;
	}
	
	public static Configuration getCustom(File file) throws ConfigurationException {
		Configuration defaults = getDefault();
		Configuration experiment = new PropertiesConfiguration(file);
		
		CompositeConfiguration cc = new CompositeConfiguration();
		cc.addConfiguration(experiment);
		cc.addConfiguration(defaults);
		
		return cc;
	}

	public static MysqlConnectionPoolDataSource getMysqlCPDS(Configuration conf) {
		MysqlConnectionPoolDataSource ds = new MysqlConnectionPoolDataSource();
		
		ds.setDatabaseName(conf.getString(MYSQL_DATABASE));
		
		if( conf.containsKey(MYSQL_HOST))
			ds.setServerName(conf.getString(MYSQL_HOST));
		
		if( conf.containsKey(MYSQL_USER))
			ds.setUser(conf.getString(MYSQL_USER));
		
		if( conf.containsKey(MYSQL_PASSWORD))			
			ds.setPassword(conf.getString(MYSQL_PASSWORD));
		
		// To avoid unexpected lost data
		ds.setStrictUpdates(false);
		
		return ds;
	}

	public static ClientConfig getClientConfig(Configuration conf) {		
		String accessKeyID = conf.getString(AWS_ACCESSKEYID); 
		String secretAccessKey = conf.getString(AWS_SECRETACCESSKEY);
		boolean sandbox = conf.getBoolean(AWS_SANDBOX, true);
		
		ClientConfig config = new ClientConfig();

		config.setAccessKeyId(accessKeyID);
		config.setSecretAccessKey(secretAccessKey);				
		config.setServiceURL(sandbox ? ClientConfig.SANDBOX_SERVICE_URL : ClientConfig.PRODUCTION_SERVICE_URL);

		return config;		
	}
	
}
