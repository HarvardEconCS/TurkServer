/**
 * 
 */
package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.ExpServerException;
import edu.harvard.econcs.turkserver.QuizFailException;
import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.SessionCompletedException;
import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.SessionOverlapException;
import edu.harvard.econcs.turkserver.SessionUnknownException;
import edu.harvard.econcs.turkserver.SimultaneousSessionsException;
import edu.harvard.econcs.turkserver.TooManyFailsException;
import edu.harvard.econcs.turkserver.TooManySessionsException;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.schema.Quiz;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.HITWorkerGroupImpl;
import edu.harvard.econcs.turkserver.server.HITWorkerImpl;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.andrewmao.misc.ConcurrentBooleanCounter;

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

	protected abstract void saveSession(Session record);
	
	/**
	 * Get all experiments in this set
	 * @return
	 */
	public abstract List<Experiment> getSetExperiments();
	
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
	public abstract List<Quiz> getSetQuizRecords(String workerId);

	/**
	 * Returns a list of all sessions in the current experiment set 
	 * associated with a worker that have had experiments completed 
	 * @param workerId
	 * @return
	 */	
	public abstract List<Session> getSetSessionInfoForWorker(String workerId);

	/**
	 * Get the assignment Id, if any, for a session
	 * @param sessionID
	 * @return
	 */	
	public abstract Session getStoredSessionInfo(String hitID);
	
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
	 * @param startTime
	 */
	protected abstract void saveExpStartTime(String expId, long startTime);

	/**
	 * Saves an experiment end time in the db
	 * @param expId
	 * @param endTime
	 */
	protected abstract void saveExpEndTime(String expId, long endTime);
	
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
	protected abstract void clearWorkerForSession(String id);

	/**
	 * Sets all sessions with NULL experiment in the DB to expired, and returns the HIT IDs
	 * @return
	 */	
	public abstract List<Session> expireUnusedSessions();	

	/**
	 * Record that an experiment has started with a set of clients
	 * @param expId
	 * @param group TODO
	 * @param startTime TODO
	 */
	public void newExperimentStarted(String expId, HITWorkerGroup group, long startTime) {				
		saveExpStartTime(expId, startTime);
		
		for( HITWorker session : group.getHITWorkers() ) {			
			saveExperiment((HITWorkerImpl) session, expId);
		}			
	}

	public void experimentFinished(String expId, HITWorkerGroup group, long endTime) {
		// Doing DB commits first so there is no limbo state
		saveExpEndTime(expId, endTime);
		
		// store final client info, with inactive time measured properly for disconnections
		for( HITWorker session : group.getHITWorkers() ) {
			saveSessionCompleteInfo((HITWorkerImpl) session);
		}		

	}	

	/**
	 * Called when the user with this ID disconnects, so appropriate cleanup can be done
	 * @param id
	 */	
	public final void sessionDisconnected(String id) {											
		/* 
		 * TODO user can accept a hit then close window, but this is the same thing as 
		 * accepting multiple hits, holding and refreshing
		 * Fix with a notification receptor later.  
		 */
		if( !(hitIsInProgress(id) || hitCompletedInDB(id)) ) {
			/* If disconnected from lobby, clear session from worker Id list
			 * also clear the username that was stored from worker
			 * 
			 * BUT if in experiment, they need to wait	
			 */
			clearWorkerForSession(id);
			idToUsername.remove(id);
		}
		
	}	

	/**
	 * Get the user name for a session
	 * User name doesn't change unless HIT returned, so store the value
	 * 
	 * @param session
	 * @return garbage if the user has no name
	 */
	public final String getUsername(HITWorkerImpl session) {
		String username = session.getUsername();
		if( username != null ) return username;
				
		return "User for " + session.getHitId();
	}
	
}
