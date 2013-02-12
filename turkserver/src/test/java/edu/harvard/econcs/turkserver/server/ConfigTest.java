package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.config.TSConfig;

public class ConfigTest {

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {		
		Configuration conf = TSConfig.getDefault();		
		assertEquals(9876, conf.getInt(TSConfig.SERVER_HTTPPORT));		
	}

}
