package edu.harvard.econcs.turkserver.server.mysql;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import edu.harvard.econcs.turkserver.ExpServerException;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.SessionRecord;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.*;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

public class MySQLDataTrackerTest {
	
	private MySQLDataTracker dt;
		
	@Before
	public void init() throws ConfigurationException {		
		
		// TODO import default schema into database
		
		MysqlConnectionPoolDataSource ds = new MysqlConnectionPoolDataSource();
		
		ds.setDatabaseName("turking");
		ds.setUser("turker");
		ds.setPassword("P@$$w0rd");
		
		// To avoid unexpected lost data
		ds.setStrictUpdates(false);	
		
		dt = new MySQLDataTracker(ds, "test");
		
	}
	
	/**
	 * Simple test of session storing capabilities
	 *  
	 * @throws SQLException
	 * @throws ExpServerException
	 * @throws UnknownHostException
	 */
	@Test
	public void testSession() throws SQLException, ExpServerException, UnknownHostException {
				
		String hitID = "HIT12340931";
		String assignmentId = "AsstIJFINGPEWRBNAE";		
		String workerId = "WorkerABJAER";
		String username = "randomuser\" WHERE";						
		
		// Add shit to database
		dt.registerAssignment(hitID, assignmentId, workerId);
		// Test for uniqueness (code copied from tracker)		
		assertTrue(dt.hitExistsInDB(hitID));		
		// Test that session is not completed
		assertFalse(dt.hitCompletedInDB(hitID));		
		
		// Check that sessionIDs stored properly 
		List<Session> srs = dt.getSetSessionInfoForWorker(workerId);
		assertTrue(srs.size() == 1);
		assertEquals(srs.iterator().next().getHitId(), hitID);
		
		// Check that assignmentID is stored correctly
		assertEquals(dt.getStoredSessionInfo(hitID).getAssignmentId(), assignmentId);
		
		dt.lobbyLogin(hitID, username);
		dt.saveIP(hitID, InetAddress.getLocalHost(), new Date());
		// Check that username cached correctly
		assertEquals(dt.getUsername(hitID), username);
		assertFalse(dt.hitCompletedInDB(hitID));
		
		// Test inactivePercent completion detection
		dt.saveSessionCompleteInfo(hitID);		
		assertTrue(dt.hitCompletedInDB(hitID));
	}
}
