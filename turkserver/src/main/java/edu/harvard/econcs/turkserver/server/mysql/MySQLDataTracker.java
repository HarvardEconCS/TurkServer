package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.schema.*;
import edu.harvard.econcs.turkserver.server.TSConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mysema.query.QueryFlag.Position;
import com.mysema.query.sql.MySQLTemplates;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLQueryImpl;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.dml.SQLDeleteClause;
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
@Singleton
public class MySQLDataTracker extends ExperimentDataTracker {	
	
	QSets _sets = QSets.sets;
	QExperiment _experiment = QExperiment.experiment;
	QSession _session = QSession.session;	
	QQuiz _quiz = QQuiz.quiz;
	QWorker _worker = QWorker.worker;
	
	private final String setID;		
	
	// TODO pool connections with C3P0
	
	private Connection conn;
	private final SQLTemplates dialect;	

	@Inject
	public MySQLDataTracker(			
			MysqlConnectionPoolDataSource ds, 
			@Named(TSConfig.EXP_SETID) String setID) {
		super();
				
		this.setID = setID;				
				
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
		// Make sure the worker table contains this workerId first, but ignore if already exists
		/*
		 * INSERT IGNORE INTO worker(id) VALUES (?)
		 */
		new SQLInsertClause(conn, dialect, _worker)
		.columns(_worker.id)
		.values(workerId)
		.addFlag(Position.START_OVERRIDE, "INSERT IGNORE INTO ")
		.execute();
		
		/*
		 * UPDATE session SET assignmentId=?, workerId=? WHERE hitId=?
		 */
		new SQLUpdateClause(conn, dialect, _session)
		.set(_session.assignmentId, assignmentId)
		.set(_session.workerId, workerId)
		.where(_session.hitId.eq(hitId))
		.execute();
				
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
	protected void saveExpStartTime(String expId, int size, String inputdata, long startTime) {
		
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
	public void clearWorkerForSession(String hitId) {
		/*
		 * TODO this used to be set to default but not sure how to do with QueryDSL
		 * UPDATE session SET workerId=DEFAULT, username=DEFAULT WHERE hitId=?
		 */
		new SQLUpdateClause(conn, dialect, _session)
		.setNull(_session.workerId)
		.setNull(_session.username)
		.where(_session.hitId.eq(hitId))
		.execute();
		
		logger.info(String.format("HIT %s has workerId cleared", hitId));
		
	}
	
	@Override
	public List<Session> expireUnusedSessions() {
		/*
		 * SELECT * FROM session WHERE setId=? AND experimentId IS NULL
		 */
		List<Session> expired = new SQLQueryImpl(conn, dialect)
		.from(_session)
		.where(_session.setId.eq(setID), _session.experimentId.isNull())
		.list(_session);
			
		logger.info("Found " + expired.size() + " unused sessions");
		
		/* 
		 * TODO this used to set to EXPIRED, but now we reuse,
		 * so we can just delete them. Verify that this is okay.  
		 * 
		 * UPDATE session SET experimentId='EXPIRED' WHERE setId=? AND experimentId IS NULL
		 */		
		new SQLDeleteClause(conn, dialect, _session)
		.where(_session.setId.eq(setID), _session.experimentId.isNull())
		.execute();
		
		return expired;
	}
	
}
