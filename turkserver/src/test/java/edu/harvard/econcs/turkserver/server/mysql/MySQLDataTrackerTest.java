package edu.harvard.econcs.turkserver.server.mysql;

import static org.junit.Assert.*;

import java.beans.PropertyVetoException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;

import edu.harvard.econcs.turkserver.ExpServerException;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.HITWorkerImpl;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;

import org.junit.*;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

public class MySQLDataTrackerTest {
	
	private MySQLDataTracker dt;
		
	@Before
	public void init() throws PropertyVetoException {		
		
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
				
		String hitId = "HIT12340931";
		String assignmentId = "AsstIJFINGPEWRBNAE";		
		String workerId = "WorkerABJAER";
		String username = "randomuser\" WHERE";						
		
		// Recorded HITId
		dt.saveHITId(hitId);
		// Test for uniqueness (code copied from tracker)		
		assertTrue(dt.hitExistsInDB(hitId));
		assertEquals(SessionStatus.UNASSIGNED,
				SessionRecord.status(dt.getStoredSessionInfo(hitId)));
		
		// Add assignment
		dt.saveAssignmentForSession(hitId, assignmentId, workerId);				
		assertEquals(SessionStatus.ASSIGNED,
				SessionRecord.status(dt.getStoredSessionInfo(hitId)));		
		
		// Check that sessionIDs stored properly 
		Collection<Session> srs = dt.getSetSessionInfoForWorker(workerId);
		assertTrue(srs.size() == 1);
		assertEquals(hitId, srs.iterator().next().getHitId());
		
		// Check that assignmentID is stored correctly
		assertEquals(assignmentId, dt.getStoredSessionInfo(hitId).getAssignmentId());
	
		HITWorkerImpl hitw = new HITWorkerImpl(null, dt.getStoredSessionInfo(hitId));
		
		dt.saveUsername(hitw, username);
		dt.saveIP(hitw, InetAddress.getLocalHost(), new Date());		
				
		// Check that username cached correctly
		assertEquals(username, dt.getStoredSessionInfo(hitId).getUsername());
		assertEquals(SessionStatus.LOBBY,
				SessionRecord.status(dt.getStoredSessionInfo(hitId)));
		
		// Test inactivePercent completion detection		
		dt.saveSessionCompleteInfo(hitw);		
		assertEquals(SessionStatus.COMPLETED,
				SessionRecord.status(dt.getStoredSessionInfo(hitId)));
	}
}
