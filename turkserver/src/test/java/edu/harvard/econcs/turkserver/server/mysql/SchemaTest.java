package edu.harvard.econcs.turkserver.server.mysql;

import static org.junit.Assert.*;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.harvard.econcs.turkserver.server.TSConfig;

public class SchemaTest {

	static Configuration conf;
	
	@BeforeClass
	public static void configure() {
		conf = TSConfig.getDefault();
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSchema() throws Exception {
		MySQLDataTracker.createSchema(conf);
	}

}
