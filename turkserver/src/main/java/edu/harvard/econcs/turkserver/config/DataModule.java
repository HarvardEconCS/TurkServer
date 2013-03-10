package edu.harvard.econcs.turkserver.config;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.amazonaws.mturk.util.ClientConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import edu.harvard.econcs.turkserver.mturk.MockRequesterService;
import edu.harvard.econcs.turkserver.mturk.RequesterServiceExt;
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
	
	@Override
	protected void configure() {		
		bind(Configuration.class).toInstance(conf);		

		// Create a single MySQL connection pool
		bind(MysqlConnectionPoolDataSource.class).toProvider(MysqlCPDSProvider.class).in(Scopes.SINGLETON);
		
		// Create AWS Requester, if any
		bind(RequesterServiceExt.class).toProvider(RequesterProvider.class).in(Scopes.SINGLETON);
		
		// GUI stuff
		bind(TSTabbedPanel.class).in(Scopes.SINGLETON);
	}
	
	public void setAWSConfig(String accessKeyId, String secretAccessKey, boolean sandbox) {
		conf.addProperty(TSConfig.AWS_ACCESSKEYID, accessKeyId);		
		conf.addProperty(TSConfig.AWS_SECRETACCESSKEY, secretAccessKey);
		conf.addProperty(TSConfig.AWS_SANDBOX, sandbox);
	}

	static class RequesterProvider implements Provider<RequesterServiceExt> {
		Configuration conf;
		
		@Inject	RequesterProvider(Configuration conf) {
			this.conf = conf;
		}
		
		@Override
		public RequesterServiceExt get() {					
			RequesterServiceExt req;
			try {
				ClientConfig reqConf = TSConfig.getClientConfig(conf);
				req = new RequesterServiceExt(reqConf);
			} catch( RuntimeException e ) {
				e.printStackTrace();
				System.out.println("Failed to create MTurk requester service. Using mock requester.");
				req = new MockRequesterService();
			}
			return req;
		}	
	}

	static class MysqlCPDSProvider implements Provider<MysqlConnectionPoolDataSource> {
		Configuration conf;
		
		@Inject	MysqlCPDSProvider(Configuration conf) {
			this.conf = conf;
		}
		
		@Override
		public MysqlConnectionPoolDataSource get() {
			return TSConfig.getMysqlCPDS(conf);			
		}	
	}
}
