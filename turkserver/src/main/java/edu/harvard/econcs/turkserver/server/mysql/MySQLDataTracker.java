package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.QuizFailException;
import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.TooManyFailsException;
import edu.harvard.econcs.turkserver.schema.*;
import edu.harvard.econcs.turkserver.server.HITWorkerImpl;
import edu.harvard.econcs.turkserver.server.QuizPolicy;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.mysema.query.QueryFlag.Position;
import com.mysema.query.sql.MySQLTemplates;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLQueryImpl;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
import com.mysema.query.types.TemplateExpressionImpl;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

/**
 * Connects to a mysql database for persistent users across sessions
 * 
 * schema for turk experiments
 * 
 * HIT ID 30 characters
 * Assignment ID 30 characters
 * Worker ID 14 characters
 * 
 * username 40 characters
 * 
 * @author mao
 *
 */
public class MySQLDataTracker extends ExperimentDataTracker {	
	
	QSets _sets = QSets.sets;
	QExperiment _experiment = QExperiment.experiment;
	QSession _session = QSession.session;	
	QQuiz _quiz = QQuiz.quiz;
	QWorker _worker = QWorker.worker;
	
	private final String setID;		
	
	// TODO remove this and pool connections with C3P0
	private final QueryRunner qr;
	
	private Connection conn;
	private final SQLTemplates dialect;
		
	static final ScalarHandler defaultScalarHandler = new ScalarHandler();
	static final ArrayListHandler defaultArrayListHandler = new ArrayListHandler();		
	static final ColumnListHandler hitIDHandler = new ColumnListHandler("hitId");	

	public MySQLDataTracker(MysqlConnectionPoolDataSource ds, String setID) {
		super();
				
		this.setID = setID;		
		this.qr = new QueryRunner(ds);
				
		dialect = new MySQLTemplates();
		
		// TODO make a proper connection		
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			conn = null;
		}
		
		// ensure this setId exists
		new SQLInsertClause(conn, dialect, _sets)
		.columns(_sets.name)
		.values(setID)
		.addFlag(Position.START_OVERRIDE, "INSERT IGNORE INTO ")
		.execute();
	}		
	
	@Override
	public List<Experiment> getSetExperiments() {		
		return new SQLQueryImpl(conn, dialect)
		.from(_experiment)
		.where(_experiment.setId.eq(setID))
		.list(_experiment);
	}

	@Override
	public boolean hitExistsInDB(String hitId) {
		long count = new SQLQueryImpl(conn, dialect)
		.from(_session)
		.where(_session.hitId.eq(hitId))
		.count();
			
		// TODO replicate expired experiment logic elsewhere
		return count > 0;
				
//		if( results != null && results.size() > 0) {
//			Object experimentId = results.get(0)[0];			
//			if( experimentId != null && "EXPIRED".equals(experimentId.toString()) ) 
//				throw new SessionExpiredException();
//			return true;
//		}
//		else return false;
	}

	@Override
	public List<Quiz> getSetQuizRecords(String workerId) {
		SQLQuery query = new SQLQueryImpl(conn, dialect);
		
		return query.from(_quiz)
				.where(_quiz.setId.eq(setID), 
						_quiz.workerId.eq(workerId))
				.list(_quiz);
	}

	@Override
	public List<Session> getSetSessionInfoForWorker(String workerId) {
		SQLQuery query = new SQLQueryImpl(conn, dialect);
		
		return query.from(_session)
				.where(_session.workerId.eq(workerId),
						_session.setId.eq(setID))
				.list(_session);
	}

	@Override
	public Session getStoredSessionInfo(String hitId) {
		List<Session> result = new SQLQueryImpl(conn, dialect)
		.from(_session)
		.where(_session.hitId.eq(hitId))
		.list(_session);
			
		// Return the first element if one exists
		return (result == null || result.size() == 0 ? null : result.get(0));
	}

	@Override
	public void saveHITId(String hitId) {
		/*
		 * INSERT INTO session (hitId, setId) VALUES (?, ?)
		 * ON DUPLICATE KEY UPDATE setId = ?
		 */
		
		new SQLInsertClause(conn, dialect, _session)
		.columns(_session.hitId, _session.setId)
		.values(hitId, setID)
		.addFlag(Position.END, TemplateExpressionImpl.create(				
				String.class, " ON DUPLICATE KEY UPDATE {0}", _session.setId.eq(setID) ))
		.execute();
	}

	@Override
	public void saveAssignmentForSession(String hitId,
			String assignmentId, String workerId) {
		try {
			// Make sure the worker table contains this workerId first, but ignore if already exists
			qr.update("INSERT IGNORE INTO worker(id) VALUES (?)", workerId);
			
			qr.update("UPDATE session SET assignmentId=?, workerId=? WHERE hitId=?", 
					assignmentId, workerId, 
					hitId);
		} catch (SQLException e) {			
			e.printStackTrace();
		}		
	}

	@Override
	public void saveQuizResults(String hitId, String workerId, QuizResults results) {					
		
		// Make sure the worker table contains this workerId first, but ignore if already exists
		new SQLInsertClause(conn, dialect, _worker)
		.columns(_worker.id)
		.values(workerId)
		.addFlag(Position.START_OVERRIDE, "INSERT IGNORE INTO ")
		.execute();
		
		/* creates
		 * INSERT IGNORE INTO worker(id) VALUES ("workerId");
		 */
		
		double score = 1.0*results.correct/results.total;
		
		new SQLInsertClause(conn, dialect, _quiz)
		.columns(_quiz.sessionId, _quiz.workerId, _quiz.setId, _quiz.numCorrect, _quiz.numTotal, _quiz.score)
		.values(hitId, workerId, setID, results.correct, results.total, score )
		.execute();				
		
	}

	@Override
	protected void saveExpStartTime(String expId, long startTime) {
		
		new SQLInsertClause(conn, dialect, _experiment)
		.columns(_experiment.id, _experiment.setId, _experiment.participants, _experiment.inputdata, _experiment.startTime)
		.values(expId, setID, size, inputdata, new Timestamp(startTime))
		.execute();				
	}

	@Override
	protected void saveExpEndTime(String expId, long endTime) {
		
		new SQLUpdateClause(conn, dialect, _experiment)
		.where(_experiment.id.eq(expId))
		.set(_experiment.endTime, new Timestamp(endTime))
		.execute();	
	}

	@Override
	protected void saveSession(Session record) {
		
		new SQLInsertClause(conn, dialect, _session)
		.populate(record)
		.addFlag(Position.START_OVERRIDE, "REPLACE ")
		.execute();
	}

	@Override
	protected void clearWorkerForSession(String id) {
		try {
			qr.update("UPDATE session SET workerId=DEFAULT, username=DEFAULT WHERE hitId=?", id);
			
			logger.info(String.format(
					"session %s has workerId cleared", id));
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
