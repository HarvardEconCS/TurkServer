package edu.harvard.econcs.turkserver.config;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.amazonaws.mturk.util.ClientConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import edu.harvard.econcs.turkserver.server.gui.TSTabbedPanel;

public class DataModule extends AbstractModule {

	protected Configuration conf;	
	
	public DataModule(String path) throws FileNotFoundException, ConfigurationException {				
		File confFile = new File(ServerModule.class.getClassLoader().getResource(path).getFile());
		if( !confFile.exists() ) throw new FileNotFoundException("configuration doesn't exist!");
		conf = TSConfig.getCustom(confFile);
		
		System.out.println("Loaded custom config file " + confFile);
	}
	
	public DataModule(Configuration conf) {
		this.conf = conf;
	}

	public DataModule() {
		conf = TSConfig.getDefault();
	}

	public Configuration getConfiguration() {		
		return conf;
	}

	/*
	 * TODO: this is a temporary method that should be removed after config is restructured
	 */
	public void setHITLimit(int some_goal) {
		conf.addProperty(TSConfig.SERVER_HITGOAL, some_goal);						
		conf.addProperty(TSConfig.EXP_REPEAT_LIMIT, some_goal);
	}
	
	@Override
	protected void configure() {		
		bind(Configuration.class).toInstance(conf);		

		// Create a single MySQL connection pool
		bind(MysqlConnectionPoolDataSource.class).toProvider(new MysqlCPDSProvider()).asEagerSingleton();
		
		// Create AWS Requester, if any
		bind(ClientConfig.class).toProvider(new ClientConfigProvider()).in(Scopes.SINGLETON);
		
		// GUI stuff
		bind(TSTabbedPanel.class).in(Scopes.SINGLETON);
	}
	
	public void setAWSConfig(String accessKeyId, String secretAccessKey, boolean sandbox) {
		conf.addProperty(TSConfig.AWS_ACCESSKEYID, accessKeyId);		
		conf.addProperty(TSConfig.AWS_SECRETACCESSKEY, secretAccessKey);
		conf.addProperty(TSConfig.AWS_SANDBOX, sandbox);
	}

	public class ClientConfigProvider implements Provider<ClientConfig> {
		@Override
		public ClientConfig get() {			
			return TSConfig.getClientConfig(conf);
		}	
	}

	public class MysqlCPDSProvider implements Provider<MysqlConnectionPoolDataSource> {
		@Override
		public MysqlConnectionPoolDataSource get() {
			return TSConfig.getMysqlCPDS(conf);			
		}	
	}
}
