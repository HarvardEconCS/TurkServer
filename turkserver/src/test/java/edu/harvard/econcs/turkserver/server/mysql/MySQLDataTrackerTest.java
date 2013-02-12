package edu.harvard.econcs.turkserver.server.mysql;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;

import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.HITWorkerImpl;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;

import org.apache.commons.configuration.Configuration;
import org.junit.*;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

public class MySQLDataTrackerTest {
	
	private static Configuration conf;
	private static MysqlConnectionPoolDataSource ds;
	
	private MySQLDataTracker dt;
		
	@BeforeClass
	public static void init() throws Exception {
		conf = TSConfig.getDefault();
		
		// import default (empty) schema into database		
		MySQLDataTracker.createSchema(conf);
		
		ds = TSConfig.getMysqlCPDS(conf);
	}
	
	@Before
	public void setup() throws Exception {			
		dt = new MySQLDataTracker(ds, "test");
	}
	
	@After
	public void tearDown() {
		dt.clearDatabase();
	}
	
	@Test // Test simple saving and retrieving from db
	public void testSaving() {		
		
		String hitId = "HIT12340931";
		String assignmentId = "AsstIJFINGPEWRBNAE";		
		String workerId = "WorkerABJAER";
		
		Session record = new Session();
		record.setHitId(hitId);
		record.setAssignmentId(assignmentId);
		record.setWorkerId(workerId);
		
		dt.saveSession(record);
		
		Session saved = dt.getStoredSessionInfo(hitId);
		
		assertEquals(hitId, saved.getHitId());
		assertEquals(assignmentId, saved.getAssignmentId());
		assertEquals(workerId, saved.getWorkerId());
	}
	
	/*
	 * Simple test of session storing capabilities
	 *  
	 */
	@Test
	public void testSession() throws Exception {
				
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
