/**
 * 
 */
package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.schema.Quiz;
import edu.harvard.econcs.turkserver.schema.Round;
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

import com.google.common.collect.Multimap;

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

	public static class SessionSummary {
		
		public final int createdHITs;
		public final int assignedHITs;
		public final int completedHITs;
		public final int submittedHITs;
		
		SessionSummary(int createdHITs, int assignedHITs, int completedHITs, int submittedHITs) {
			this.createdHITs = createdHITs;
			this.assignedHITs = assignedHITs;
			this.completedHITs = completedHITs;
			this.submittedHITs = submittedHITs;
		}
	}

	/**
	 * Get all experiments in this set
	 * @return
	 */
	public abstract Collection<Experiment> getSetExperiments();
	
	/**
	 * Get experiment data
	 * @param experimentId
	 * @return
	 */
	public abstract Experiment getExperiment(String experimentId);

	/**
	 * Get all experiments and corresponding sessions
	 * @return
	 */
	public abstract Multimap<Experiment, Session> getAllExperimentSessions();

	/**
	 * Get all rounds for an experiment
	 * @param experimentId
	 * @return
	 */
	public abstract List<Round> getExperimentRounds(String experimentId);

	/**
	 * Get all experiments and corresponding rounds
	 * @return
	 */
	public abstract Multimap<Experiment, Round> getAllExperimentRounds();

	/**
	 * Get all completed sessions in this set.
	 * @return
	 */
	public abstract List<Session> getCompletedSessions();

	/**
	 * Check if we have a record of a sessionID in the database,
	 * including from other sets
	 * @param hitID
	 * @return
	 */	
	public abstract boolean hitExistsInDB(String hitID);

	/**
	 * Get information about currently recorded HITs in the database
	 * @return
	 */
	public abstract SessionSummary getSetSessionSummary();

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
	public abstract Session getStoredSessionInfo(String hitId);
	
	/**
	 * To be implemented method to save the session for a tracker
	 * @param record
	 */
	public abstract void saveSession(Session record);

	/**
	 * Adds a hitId to the current set in the database
	 * @param hitId
	 */
	public abstract void saveHITId(String hitId);

	/**
	 * Save the results of a quiz
	 * @param hitId
	 * @param workerId
	 * @param qr 
	 */
	public abstract void saveQuizResults(String hitId, String workerId, Quiz qr);

	/**
	 * Associate an assignment and worker Id to a session
	 * @param hitId
	 * @param assignmentId
	 * @param workerId
	 */	
	public void saveWorkerAssignment(HITWorkerImpl session, String assignmentId, String workerId) {		
		Session record = session.getSessionRecord();
		
		record.setAssignmentId(assignmentId);
		record.setWorkerId(workerId);
		
		saveSession(record);
	}

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
	public final void saveBonusAmount(HITWorkerImpl hitWorker, double amount) {
		Session record = hitWorker.getSessionRecord();
			
		record.setBonus(BigDecimal.valueOf(amount));		
		
		saveSession(record);
	}

	/**
	 * Saves the experiment that a session started
	 * @param session
	 * @param experimentID
	 */
	public void saveExperiment(HITWorkerImpl session, String experimentID) {
		Session record = session.getSessionRecord();		
		record.setExperimentId(experimentID);		
		saveSession(record);
	}

	/**
	 * Save the answers in the exit survey
	 * @param hitId
	 * @param workerId
	 * @param comments
	 */
	public void saveExitSurveyResults(HITWorkerImpl session, String comments) {
		Session record = session.getSessionRecord();		
		record.setComment(comments);
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
	 * Save the treatment data for a round, if any
	 * @param expId
	 * @param currentRound
	 * @param inputData
	 */
	protected abstract void saveExpRoundInput(String expId, int currentRound, String inputData);

	/**
	 * Save the results of a round to the database
	 * @param expId
	 * @param round
	 * @param roundLog
	 */
	protected abstract void saveExpRoundEnd(String expId, int round, long endTime, String roundLog);

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
		
		record.setInactiveData(session.getInactiveInfo());
		record.setInactivePercent(session.getInactivePercent());
		record.setNumDisconnects(session.getNumDisconnects());
		
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
	 * Removes a session record from the database
	 * @param hitId
	 * @return true if the session was deleted
	 */
	public abstract boolean deleteSession(String hitId);	

	/**
	 * Deletes HIT IDs that are not in experiment or completed from database, and returns them 
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

	public void saveRoundInput(ExperimentControllerImpl expCont, String inputData) {
		saveExpRoundInput(expCont.getExpId(), expCont.getCurrentRound(), inputData);		
	}

	public final void experimentRoundComplete(ExperimentControllerImpl expCont, long endTime, String roundLog) {
		saveExpRoundEnd(expCont.getExpId(), expCont.getCurrentRound(), endTime, roundLog);		
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
			HITWorkerImpl hitw = (HITWorkerImpl) session;
			hitw.finalizeActivity();
			saveSessionCompleteInfo(hitw);
		}		

	}	
	
}
