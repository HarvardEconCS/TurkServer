package edu.harvard.econcs.turkserver.server.mysql;

import java.beans.PropertyVetoException;

import org.apache.commons.configuration.Configuration;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import edu.harvard.econcs.turkserver.config.TSConfig;

public class ClearDatabase {

	/**
	 * @param args
	 * @throws PropertyVetoException 
	 */
	public static void main(String[] args) throws PropertyVetoException {
		Configuration conf = TSConfig.getDefault();
		
		MysqlConnectionPoolDataSource ds = TSConfig.getMysqlCPDS(conf);

		MySQLDataTracker dt = new MySQLDataTracker(ds);		
		
		dt.clearDatabase();
	}

}
