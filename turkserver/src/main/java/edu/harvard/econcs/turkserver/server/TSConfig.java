package edu.harvard.econcs.turkserver.server;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

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
	
	public static final String CONCURRENCY_LIMIT = "session.concurrent.limit";
	
	/* ****************************************************
	 * Default MTurk values
	 ******************************************************/
	
	
	
	/* ****************************************************
	 * Named parameters
	 ******************************************************/
	
	public static final String EXP_INIT = "experiment.initializer";
	public static final String EXP_CLASS = "experiment.class";	
	public static final String EXP_SETID = "experiment.setid";
	
	public static final String SET_REPEAT_LIMIT = "session.set.limit";
	
	public static final String SPECIAL_WORKERS = "experiment.special.workers";
	public static final String INPUT_LIST = "experiment.inputdata";
	
	/* ****************************************************
	 * Other things that will need to be set
	 ******************************************************/	
	
	public static final String MYSQL_HOST = "mysql.host";
	public static final String MYSQL_DATABASE = "mysql.database";
	public static final String MYSQL_USER = "mysql.username";
	public static final String MYSQL_PASSWORD = "mysql.password";
	
	public static final String SERVER_HITGOAL = "server.hitgoal";
	public static final String SERVER_USERNAME = "server.usernames";
	public static final String SERVER_LOBBY = "server.lobby";
	
	public static final String HITS_INITIAL = "server.hitinitial";
	public static final String HITS_DELAY = "server.hitdelay";
	public static final String HITS_TOTAL = "server.hittotal";							
	
	public static Configuration getDefault() {
		Configuration conf = null;
		try {
			conf = new PropertiesConfiguration(TURKSERVER_CONFIG);
			System.out.printf("Found and loaded %s\n", TURKSERVER_CONFIG);
		} catch (ConfigurationException e) {
			System.out.printf("Unable to load %s. Proceeding with default settings...\n", TURKSERVER_CONFIG);
			conf = new PropertiesConfiguration();
		}		
				
		conf.addProperty(SERVER_HTTPPORT, 9876);				
		conf.addProperty(SERVER_USERNAME, false);
		
		return conf;
	}
	
	public static Configuration getCustom(String file) throws ConfigurationException {
		Configuration defaults = getDefault();
		Configuration experiment = new PropertiesConfiguration(file);
		
		CompositeConfiguration cc = new CompositeConfiguration();
		cc.addConfiguration(experiment);
		cc.addConfiguration(defaults);
		
		return cc;
	}
	
}
