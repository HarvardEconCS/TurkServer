/**
 * 
 */
package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.schema.Quiz;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.ExperimentControllerImpl;
import edu.harvard.econcs.turkserver.server.HITWorkerImpl;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to keep track of users and HITs. Must be thread safe.
 * Can be database-backed or persist in memory.
 * 
 * @author mao
 *
 */
public abstract class ExperimentDataTracker {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	public static final int USERNAME_LIMIT = 40;			

	/**
	 * Get all experiments in this set
	 * @return
	 */
	public abstract Collection<Experiment> getSetExperiments();
	
	/**
	 * Check if we have a record of a sessionID in the database,
	 * including from other sets
	 * @param hitID
	 * @return
	 */	
	public abstract boolean hitExistsInDB(String hitID);

	/**
	 * Get the quiz results in the current set for the worker in question
	 * @param workerId
	 * @return
	 */
	public abstract Collection<Quiz> getSetQuizRecords(String workerId);

	/**
	 * Returns a list of all sessions in the current experiment set 
	 * associated with a worker that have had experiments completed 
	 * @param workerId
	 * @return
	 */	
	public abstract Collection<Session> getSetSessionInfoForWorker(String workerId);

	/**
	 * Get the assignment Id, if any, for a session
	 * @param sessionID
	 * @return
	 */	
	public abstract Session getStoredSessionInfo(String hitID);
	
	abstract void saveSession(Session record);

	/**
	 * Adds a hitId to the current set in the database
	 * @param hitId
	 */
	public abstract void saveHITId(String hitId);

	/**
	 * Associate an assignment and worker Id to a session
	 * @param hitId
	 * @param assignmentId
	 * @param workerId
	 */	
	public abstract void saveAssignmentForSession(String hitId, String assignmentId, String workerId);

	/**
	 * Save the results of a quiz
	 * @param hitId
	 * @param workerId
	 * @param qr 
	 */
	public abstract void saveQuizResults(String hitId, String workerId, QuizResults qr);

	/**
	 * Associate a user name to a session (only for the lobby anyway)
	 * Can also store the time this happened
	 * @param session
	 * @param username
	 */
	public void saveUsername(HITWorkerImpl session, String username) {
		// Update with username and the time they entered the lobby
		// TODO worry about previous users for this session's lobby Time
		
		// TODO automatic truncation right now but we should fix this in frontend
		if( username.length() > USERNAME_LIMIT ) username = username.substring(0, USERNAME_LIMIT);
		
		Session record = session.getSessionRecord();
				
		record.setUsername(username);
		
		saveSession(record);		
	}
		
	/**
	 * Saves the IP address for a given session
	 * @param session
	 * @param remoteSocketAddress
	 */	
	public void saveIP(HITWorkerImpl session, InetAddress remoteAddress, Date lobbyTime) {			
		Session record = session.getSessionRecord();
		
		record.setIpAddr(remoteAddress.getHostAddress());
		record.setLobbyTime(new Timestamp(lobbyTime.getTime()));
		
		saveSession(record);
	}

	/**
	 * Save a bonus that will be paid to a particular worker
	 * @param hitWorker
	 * @param amount
	 */
	public void saveBonusAmount(HITWorkerImpl hitWorker, double amount) {
		Session record = hitWorker.getSessionRecord();
			
		record.setBonus(BigDecimal.valueOf(amount));		
		
		saveSession(record);
	}

	/**
	 * Saves the experiment that a session started
	 * @param session
	 * @param experimentID
	 */
	protected void saveExperiment(HITWorkerImpl session, String experimentID) {
		Session record = session.getSessionRecord();
		
		record.setExperimentId(experimentID);
		
		saveSession(record);
	}

	/**
	 * Saves an experiment start time in the db
	 * @param expId
	 * @param size
	 * @param inputdata
	 * @param startTime
	 */
	protected abstract void saveExpStartTime(String expId, int size, String inputdata, long startTime);

	/**
	 * Initialize a round in the database
	 * @param expId
	 * @param currentRound
	 */
	protected abstract void saveExpRoundStart(String expId, int round, long startTime);

	/**
	 * Save the results of a round to the database
	 * @param expId
	 * @param round
	 * @param roundLog
	 */
	protected abstract void saveExpRoundEnd(String expId, long endTime, int round, String roundLog);

	/**
	 * Saves an experiment end time in the db
	 * @param expId
	 * @param endTime
	 * @param logOutput 
	 */
	protected abstract void saveExpEndInfo(String expId, long endTime, String logOutput);
	
	/**
	 * Saves the amount of time (percent) a client was inactive over the entire experiment
	 *  
	 * @param session
	 */
	protected void saveSessionCompleteInfo(HITWorkerImpl session) {
		Session record = session.getSessionRecord();
		
		record.setInactiveData(session.getLiveInactiveDescriptor());
		record.setInactivePercent(session.getLiveInactivePercent());
		record.setNumDisconnects(session.getLiveNumDisconnects());
		
		saveSession(record);
	}
	
	/**
	 * Clears the worker for a session, as well as username. 
	 * However, does not clear the assignment,
	 * which is reset when someone else connects
	 * @param id
	 */
	public abstract void clearWorkerForSession(String id);

	/**
	 * Sets all sessions with NULL experiment in the DB to expired, and returns the HIT IDs
	 * @return
	 */	
	public abstract List<Session> expireUnusedSessions();	

	/**
	 * Record that an experiment has started with a set of clients
	 * @param expId
	 * @param group 
	 * @param startTime 
	 */
	public final void newExperimentStarted(ExperimentControllerImpl expCont) {
		String expId = expCont.getExpId();
		saveExpStartTime(expId, expCont.getGroup().groupSize(), expCont.getInputData(), expCont.getStartTime());
		
		for( HITWorker session : expCont.getGroup().getHITWorkers() ) {			
			saveExperiment((HITWorkerImpl) session, expId);
		}			
	}

	public void experimentRoundStarted(ExperimentControllerImpl expCont, long startTime) {		
		saveExpRoundStart(expCont.getExpId(), expCont.getCurrentRound(), startTime);
	}

	public final void experimentRoundComplete(ExperimentControllerImpl expCont, long endTime, String roundLog) {
		saveExpRoundEnd(expCont.getExpId(), endTime, expCont.getCurrentRound(), roundLog);		
	}

	/**
	 * Record that experiment is finished and log output
	 * @param expCont
	 * @param logOutput
	 */
	public final void experimentFinished(ExperimentControllerImpl expCont, String logOutput) {
		String expId = expCont.getExpId();
		// Doing DB commits first so there is no limbo state
		saveExpEndInfo(expId, expCont.getFinishTime(), logOutput);
		
		// store final client info, with inactive time measured properly for disconnections
		for( HITWorker session : expCont.getGroup().getHITWorkers() ) {
			saveSessionCompleteInfo((HITWorkerImpl) session);
		}		

	}	
	
}
