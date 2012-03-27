package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.QuizFailException;
import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.TooManyFailsException;
import edu.harvard.econcs.turkserver.server.ExperimentServer;
import edu.harvard.econcs.turkserver.server.HostServer;
import edu.harvard.econcs.turkserver.server.QuizMaster;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;

import java.math.BigInteger;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

/**
 * Connects to a mysql database for persistent users across sessions
 * @author mao
 *
 */
public class MySQLDataTracker extends ExperimentDataTracker {
	
	public static final int USERNAME_LIMIT = 40;
	
	private final String setID;	
	
	private final QuizMaster quizMaster;
	
	private final QueryRunner qr;
		
	static final ScalarHandler defaultScalarHandler = new ScalarHandler();
	static final ArrayListHandler defaultArrayListHandler = new ArrayListHandler();	
	static final BigIntegerListHandler defaultIDHandler = new BigIntegerListHandler("id");	
	static final ColumnListHandler hitIDHandler = new ColumnListHandler("hitId");
	static final SessionRecordListHandler sessionHandler = new SessionRecordListHandler();

	public MySQLDataTracker(MysqlConnectionPoolDataSource ds, String setID, 
			QuizMaster qm, int simultaneousSessionLimit, int totalSetLimit) {
		super(simultaneousSessionLimit, totalSetLimit);
		
		this.setID = setID;
		this.quizMaster = qm;
		this.qr = new QueryRunner(ds);
		
		// TODO ensure this setId exists
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
	public void createSchema(boolean dropOld) {
		String query;
		
		try {
			if( dropOld ) {
				query = "DROP TABLE IF EXISTS quiz, qual, session, experiment, worker"; 
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
			
			// Experiments table
			query = 
				"CREATE TABLE IF NOT EXISTS experiment (" +
				"id VARCHAR(24) NOT NULL PRIMARY KEY," +
				"setId VARCHAR(24)," +
				"players INT," +
				"file VARCHAR(24)," +
				"startTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
				"endTime TIMESTAMP DEFAULT 0," +
				"FOREIGN KEY(setId) REFERENCES sets(name) ON UPDATE CASCADE" +				
				") ENGINE = InnoDB";
			debugUpdate(query);
			
			// Special key for expired sessions
			query = 
				"INSERT IGNORE INTO experiment (id, startTime) VALUES (\"EXPIRED\", 0)";
			debugUpdate(query);
			
			// Session table - use innodb for row locking
			query = 
				"CREATE TABLE IF NOT EXISTS session (" +
				"id VARCHAR(40) NOT NULL PRIMARY KEY," +
				"setId VARCHAR(24)," +
				"hitId VARCHAR(30) UNIQUE," +
				"assignmentId VARCHAR(30) UNIQUE," +
				"workerId VARCHAR(14)," +
				"username VARCHAR(40)," +
				"lobbyTime TIMESTAMP DEFAULT 0," +
				"ipAddr VARCHAR(16)," +
				"experimentId VARCHAR(24)," +
				"inactivePercent DOUBLE," +
				"paid DECIMAL(10,2)," +
				"bonusPaid DECIMAL(10,2)," +
				"hitStatus VARCHAR(16)," +
				"comment TEXT," +
				"FOREIGN KEY(setId) REFERENCES sets(name) ON UPDATE CASCADE," +
				"FOREIGN KEY(workerId) REFERENCES worker(id)," +
				"FOREIGN KEY(experimentId) REFERENCES experiment(id)" +
				") ENGINE = InnoDB";
			debugUpdate(query);
			
			// Quiz results table
			query = 
				"CREATE TABLE IF NOT EXISTS quiz (" +
				"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
				"sessionId VARCHAR(40)," +
				"workerId VARCHAR(14)," +
				"setId VARCHAR(24)," +
				"numCorrect INT DEFAULT 0," +
				"numTotal INT DEFAULT 0," +
				"FOREIGN KEY(sessionId) REFERENCES session(id) ON DELETE CASCADE," +
				"FOREIGN KEY(workerId) REFERENCES worker(id)" +
				") ENGINE = InnoDB";
			debugUpdate(query);
			
			query = 
				"CREATE TABLE IF NOT EXISTS qual (" +
				"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
				"workerId VARCHAR(40)," +
				"qual VARCHAR(30)," +
				"value INT NOT NULL," +
				"FOREIGN KEY(workerId) REFERENCES worker(id), " +
				"UNIQUE (workerId, qual) " +
				") ENGINE = InnoDB";
			debugUpdate(query);
			
			logger.info("Created new schema");		
		} catch (SQLException e) {			
			e.printStackTrace();
		}				
	}
	
	private void debugUpdate(String query, Object... params) throws SQLException {
		System.out.println(query);		
		System.out.println(qr.update(query, params) + " rows updated");
	}

	@Override
	public BigInteger getNewSessionID() {
		BigInteger newID = null;
		
		try { 
			do {
				newID = new BigInteger(HostServer.ID_LEN, rnd);				
			} while( sessionExistsInDB(newID) );
			
			// Save the new ID that we generated
			qr.update("INSERT INTO session(id, setId) VALUES (?, ?)", 
					newID.toString(16), setID);			
		} catch( SQLException e ) {
			e.printStackTrace();
		} catch (SessionExpiredException e) {			
			e.printStackTrace();
		}
		
		return newID;
	}
	
	@Override
	public boolean sessionExistsInDB(BigInteger sessionID) throws SessionExpiredException {
		List<Object[]> results = null;
		
		try {
			results = qr.query("SELECT experimentId FROM session WHERE id=?", 
					defaultArrayListHandler, 
					sessionID.toString(16));
		} catch (SQLException e) {			
			e.printStackTrace();
		}
				
		if( results != null && results.size() > 0) {
			Object experimentId = results.get(0)[0];			
			if( experimentId != null && "EXPIRED".equals(experimentId.toString()) ) 
				throw new SessionExpiredException();
			return true;
		}
		else return false;
	}
	
	@Override
	protected boolean userRequiresQuiz(String workerId)
			throws TooManyFailsException {
		if( quizMaster == null ) return false;
		
		Object result = null;
		List<Object[]> results = null;
		
		// See if there are any perfect scores in this set
		try {
			result = qr.query("SELECT COUNT(*) FROM quiz WHERE numCorrect = numTotal " +
					"AND workerId=? AND setId=?", defaultScalarHandler, workerId, setID);
		} catch (SQLException e) {			
			e.printStackTrace();
		}
		
		if( result != null && Integer.parseInt(result.toString()) > 0) return false;
		
		// See if we should let them have another try
		try {
			results = qr.query("SELECT sum(numCorrect), sum(numTotal) FROM quiz WHERE workerId=? AND setId=?", 
					defaultArrayListHandler, workerId, setID);
		} catch (SQLException e) {			
			e.printStackTrace();
		}
				
		if( results != null && results.size() > 0) {
			Object correctCount = results.get(0)[0];
			Object totalCount = results.get(0)[1];
			int numCorrect = correctCount == null ? 0 : Integer.parseInt(correctCount.toString());
			int numTotal = totalCount == null ? 0 : Integer.parseInt(totalCount.toString());

			if( numTotal == 0 || // First timer
					!quizMaster.overallFail(numCorrect, numTotal)) 
				return true;
			else 
				throw new TooManyFailsException();
		}
		
		return true;
	}

	@Override
	public boolean sessionCompletedInDB(BigInteger sessionID) {		
		SessionRecord sr = getStoredSessionInfo(sessionID);
		
		return (sr != null && (sr.getStatus() == SessionStatus.COMPLETED));
	}

	@Override
	public List<SessionRecord> getSetSessionInfoForWorker(String workerId) {
		List<SessionRecord> results = null;
		
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
	public SessionRecord getStoredSessionInfo(BigInteger sessionID) {
		List<SessionRecord> result = null;
		
		try {
			result = qr.query("SELECT * FROM session WHERE id=?",
					sessionHandler, 
					sessionID.toString(16));
		} catch (SQLException e) {			
			e.printStackTrace();
		} 
			
		// Return the first element if one exists
		return (result == null || result.size() == 0 ? null : result.get(0));
	}

	@Override
	public void saveHITIdForSession(BigInteger sessionID, String hitId) {		
		try {
			qr.update("UPDATE session SET hitId=? WHERE id=?", 
					hitId, 
					sessionID.toString(16));
		} catch (SQLException e) {			
			e.printStackTrace();
		}		
	}

	@Override
	public void saveAssignmentForSession(BigInteger sessionID,
			String assignmentId, String workerId) {
		try {
			// Make sure the worker table contains this workerId first, but ignore if already exists
			qr.update("INSERT IGNORE INTO worker(id) VALUES (?)", workerId);
			
			qr.update("UPDATE session SET assignmentId=?, workerId=? WHERE id=?", 
					assignmentId, workerId, 
					sessionID.toString(16));
		} catch (SQLException e) {			
			e.printStackTrace();
		}		
	}

	@Override
	public void saveQuizResults(BigInteger sessionID, QuizResults results) throws QuizFailException {
		if( quizMaster == null ) {
			logger.severe("Got back quiz results with no quiz master?");
			return;
		}
		
		String workerId = "unidentified";
		try {
			workerId = qr.query("SELECT workerId FROM session WHERE id=?", defaultScalarHandler, sessionID.toString(16)).toString();
			
			// Make sure the worker table contains this workerId first, but ignore if already exists
			qr.update("INSERT IGNORE INTO worker(id) VALUES (?)", workerId);
			
			qr.update("INSERT INTO quiz(sessionId, workerId, setId, numCorrect, numTotal) " +
					"VALUES (?, ?, ?, ?, ?)",
					sessionID.toString(16), workerId, setID, results.correct, results.total
					);
		} catch (SQLException e) {			
			e.printStackTrace();
		}
		
		if( !quizMaster.quizPasses(results) ) {
			logger.info(workerId + " failed quiz");
			throw new QuizFailException();
		}
		else {
			logger.info(workerId + " passed quiz");
		}
	}

	@Override
	protected void saveUsernameForSession(BigInteger sessionId, String username) {		
		try {
			// Update with username and the time they entered the lobby
			// TODO worry about previous users for this session's lobby Time
			
			// TODO automatic truncation right now but we should fix this in frontend
			if( username.length() > USERNAME_LIMIT ) username = username.substring(0, USERNAME_LIMIT);
			
			qr.update("UPDATE session SET username=? WHERE id=?",
					username, 
					sessionId.toString(16));
		} catch (SQLException e) {			
			e.printStackTrace();
		}		
	}

	@Override
	public void saveIPForSession(BigInteger id, InetAddress remoteAddress, Date lobbyTime) {
		try {
			qr.update("UPDATE session SET ipAddr=?, lobbyTime=? WHERE id=?", 
					remoteAddress.getHostAddress(), lobbyTime,
					id.toString(16));
		} catch (SQLException e) {			
			e.printStackTrace();
		}		
	}

	@Override
	protected void saveExpStartTime(ExperimentServer<?> exp, Date startTime) {
		try {
			String filename = exp.getFilename();
			qr.update("INSERT INTO experiment (id, setId, players, file, startTime) VALUES (?,?,?,?,?)",
					exp.experimentID, setID, exp.size(), filename, startTime);
		} catch (SQLException e) {			
			e.printStackTrace();
		}
	}

	@Override
	protected void saveExperimentForSession(BigInteger sessionID,
			String experimentID) {
		try {
			qr.update("UPDATE session SET experimentId=? WHERE id=?",
					experimentID, sessionID.toString(16));
		} catch (SQLException e) {			
			e.printStackTrace();
		}
		
	}

	@Override
	protected void saveExpEndTime(ExperimentServer<?> exp, Date endTime) {
		try {
			qr.update("UPDATE experiment SET endTime=? where id=?",
					endTime, exp.experimentID);
		} catch (SQLException e) {			
			e.printStackTrace();
		}
		
	}

	@Override
	protected void saveSessionCompleteInfo(BigInteger sessionID,
			double inactivePercent) {
		try {
			qr.update("UPDATE session SET inactivePercent=? WHERE id=?", 
					inactivePercent, 
					sessionID.toString(16));
		} catch (SQLException e) {			
			e.printStackTrace();
		}		
	}

	@Override
	protected void clearWorkerForSession(BigInteger id) {
		try {
			qr.update("UPDATE session SET workerId=DEFAULT, username=DEFAULT WHERE id=?", id.toString(16));
			
			logger.info(String.format(
					"session %s has workerId cleared", id.toString(16)));
		} catch (SQLException e) {			
			e.printStackTrace();
		}		
	}
	
	@Override
	public List<SessionRecord> expireUnusedSessions() {		
		List<SessionRecord> expired = null;
		
		// TODO replace this with better sql
		try {
			// Get the list first
			expired = qr.query(
					"SELECT hitId FROM session WHERE setId=? AND experimentId IS NULL", 
					sessionHandler, setID);
			
			// Now update
			qr.update("UPDATE session SET experimentId='EXPIRED' WHERE setId=? AND experimentId IS NULL", 
					setID);
			
			logger.info("Found " + expired.size() + " unused sessions");

		} catch (SQLException e) {			
			e.printStackTrace();
		}
		
		return expired;
	}
	
}
