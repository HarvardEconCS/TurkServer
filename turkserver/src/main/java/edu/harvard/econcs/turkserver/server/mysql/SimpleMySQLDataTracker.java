/**
 * 
 */
package edu.harvard.econcs.turkserver.server.mysql;

import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.AbstractListHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;

/**
 * @author mao
 *
 */
@Deprecated
public class SimpleMySQLDataTracker extends SimpleDataTracker {
	
	private final String setID;	
	private final QueryRunner qr;
	
	static final ScalarHandler defaultScalarHandler = new ScalarHandler();
	static final ArrayListHandler defaultArrayListHandler = new ArrayListHandler();	
	
	static final ColumnListHandler defaultColumnListHandler = new ColumnListHandler();
	
	static final StringListHandler defaultIdHandler = new StringListHandler("hitId");
	static final StringListHandler defaultDataHandler = new StringListHandler("data");
		
	static final AbstractListHandler<Session> sessionHandler = new AbstractListHandler<Session>() {
		@Override
		protected Session handleRow(ResultSet rs) throws SQLException {
			// TODO remove all this stuff
			return null;
		}		
	};
	
	public SimpleMySQLDataTracker(MysqlConnectionPoolDataSource ds, String setID,
			int simultaneousSessionLimit, int totalSetLimit) {
		super(simultaneousSessionLimit, totalSetLimit);
		
		this.setID = setID;
		this.qr = new QueryRunner(ds);
	}

	/**
	 * Creates a new schema for turk experiments
	 * 
	 * Session ID 40 characters
	 * HIT ID 30 characters
	 * Assignment ID 30 characters
	 * Worker ID 14 characters
	 * 
	 * username 40 characters
	 * 
	 * @param dropOld whether to drop the old database
	 */
	public void createSchema(boolean dropOld) throws SQLException {
		String query;


		if( dropOld ) {
			query = "DROP TABLE IF EXISTS session, sets, worker"; 
			debugUpdate(query);
		}

		// Workers table
		query = 
				"CREATE TABLE IF NOT EXISTS worker (" +
						"id VARCHAR(14) NOT NULL PRIMARY KEY," +
						"notify ENUM('off', 'on')" +				
						") ENGINE = InnoDB";
		debugUpdate(query);

		// Sets table
		query = 
				"CREATE TABLE IF NOT EXISTS sets (" +
						"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
						"name VARCHAR(24) NOT NULL UNIQUE KEY," +
						"descript TEXT" +
						") ENGINE = InnoDB";
		debugUpdate(query);

		// Make sure set exists already for FK constraints
		query =
				"INSERT IGNORE INTO sets (name) VALUES (?)";
		debugUpdate(query, setID);								

		// Session table - use innodb for row locking
		query = 
				"CREATE TABLE IF NOT EXISTS session (" +
						"hitId VARCHAR(30) NOT NULL PRIMARY KEY," +
						"setId VARCHAR(24)," +
						"data VARCHAR(24)," +
						"assignmentId VARCHAR(30) UNIQUE," +
						"workerId VARCHAR(14)," +				
						"joinTime TIMESTAMP DEFAULT 0," +
						"ipAddr VARCHAR(16)," +				
						"inactivePercent DOUBLE," +
						"paid DECIMAL(10,2)," +
						"bonusPaid DECIMAL(10,2)," +
						"hitStatus VARCHAR(16)," +
						"results TEXT," +
						"comment TEXT," +
						"FOREIGN KEY(setId) REFERENCES sets(name) ON UPDATE CASCADE," +
						"FOREIGN KEY(workerId) REFERENCES worker(id)" +				
						") ENGINE = InnoDB";
		debugUpdate(query);			

		logger.info("Created new schema");		
					
	}
	
	private void debugUpdate(String query, Object... params) throws SQLException {
		System.out.println(query);		
		System.out.println(qr.update(query, params) + " rows updated");
	}	

	@Override
	public boolean sessionExistsInDB(String hitID) throws SessionExpiredException {		
		List<Object> results = null;
		
		try {
			results = qr.query("SELECT hitStatus FROM session WHERE hitId=?", 
					defaultColumnListHandler, 
					hitID);
		} catch (SQLException e) {			
			e.printStackTrace();
		}
		
		// Check if this session has a result already, if so then it's expired
		if( results != null && results.size() > 0) {
			Object obj = results.get(0);			
			if( obj != null && obj.toString().equals("EXPIRED")) 
				throw new SessionExpiredException();
			return true;
		}
		
		else return false;
	}

	@Override
	public boolean sessionCompletedInDB(String hitID) {
		Session sr = getStoredSessionInfo(hitID);
		
		return (sr != null && (SessionRecord.status(sr) == SessionStatus.COMPLETED));
	}

	@Override
	public List<Session> getSetSessionInfoForWorker(String workerId) {
		List<Session> results = null;
		
		try {
			// This should be quick because workerId is indexed as a foreign key
			results = qr.query("SELECT * FROM session WHERE workerId=? AND setId=?", 
					sessionHandler, 
					workerId, setID);
		} catch (SQLException e) {			
			e.printStackTrace();
		}
		
		return results;
	}
	
	@Override
	public List<Object[]> getWorkerAndTotalDataCounts(String workerId) {		
		
		try {
			List<Object[]> dataObj = qr.query(
					"SELECT data, " +
					"SUM(CASE WHEN isNull(workerId) THEN 0 ELSE workerId=? END) as sum, " +
					"COUNT(*) as ct FROM session " +
					"WHERE setId=? AND data IS NOT NULL " +
					"GROUP BY data " +
					"ORDER BY ct ASC", 
					defaultArrayListHandler, 
					workerId, setID);
			
			return dataObj;
			
		} catch (SQLException e) {			
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public Session getStoredSessionInfo(String hitID) {
		List<Session> result = null;
		
		try {
			result = qr.query("SELECT * FROM session WHERE hitId=?",
					sessionHandler, 
					hitID);
		} catch (SQLException e) {			
			e.printStackTrace();
		} 
			
		// Return the first element if one exists
		return (result == null || result.size() == 0 ? null : result.get(0));
	}	

	@Override
	public void saveHITId(String hitId) {		
		try {
			qr.update("INSERT INTO session (hitId, setId) VALUES (?, ?) " +
					"ON DUPLICATE KEY UPDATE setId=?", hitId, setID, setID);
		} catch (SQLException e) {			
			e.printStackTrace();
		} 		
	}

	@Override
	public void saveAssignmentForSession(String hitID, String assignmentId, String workerId) {
		try { 
			// Make sure the worker table contains this workerId first, but ignore if already exists
			qr.update("INSERT IGNORE INTO worker(id) VALUES (?)", workerId);

			qr.update("UPDATE session SET assignmentId=?, workerId=? WHERE hitId=?", 
					assignmentId, workerId,	hitID);
		} catch (SQLException e) {			
			e.printStackTrace();
		}	
	}

	@Override
	public void saveIPForSession(String hitID, InetAddress remoteAddress,
			Date lobbyTime) {
		try {
			qr.update("UPDATE session SET ipAddr=?, joinTime=? WHERE hitId=?", 
					remoteAddress.getHostAddress(), lobbyTime, hitID);
		} catch (SQLException e) {			
			e.printStackTrace();
		}		
	}

	@Override
	public void setSessionData(String hitId, String data) {
		try {
			qr.update("UPDATE session SET data=? WHERE hitId=?",
					data, hitId);
		} catch (SQLException e) {			
			e.printStackTrace();
		}	
	}

	@Override
	public void saveSessionLog(String hitID, String data) {
		try {
			qr.update("UPDATE session SET results=?, inactivePercent=? WHERE hitId=?", 
					data, 0.0, hitID);
		} catch (SQLException e) {			
			e.printStackTrace();
		}	
	}

	@Override
	public List<Session> expireUnusedSessions() {		
		List<Session> expired = null;
		
		// TODO replace this with better sql
		try {
			// Get the list first
			expired = qr.query(
					"SELECT * FROM session WHERE setId=? AND results IS NULL", 
					sessionHandler, setID);
			
			// Now update
			qr.update("UPDATE session SET hitStatus='EXPIRED' WHERE setId=? AND results IS NULL", 
					setID);
		
			logger.info("Found " + expired.size() + " unused sessions");
		} catch (SQLException e) {			
			e.printStackTrace();
		}				
		
		return expired;
	}

}
