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
import edu.harvard.econcs.turkserver.Codec.LoginStatus;
import edu.harvard.econcs.turkserver.server.ExperimentServer;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.andrewmao.misc.ConcurrentBooleanCounter;

/**
 * A class to keep track of users and HITs. Must be thread safe.
 * @author mao
 *
 */
/**
 * @author mao
 *
 * @param <T>
 */
public abstract class ExperimentDataTracker implements DataTracker<String> {

	protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());	
				
	private final int simultaneousSessionLimit;
	private final int totalSetLimit;
			
	// Username tracking - cached for faster access by HostServer		
	private ConcurrentHashMap<String, String> idToUsername;
		
	// Experiment tracking - FALSE if in progress and TRUE if finished
	protected final ConcurrentBooleanCounter<ExperimentServer<?>> experiments; 
	protected final ConcurrentHashMap<String, ExperimentServer<?>> idToExp;	
	
	protected ExperimentDataTracker(int simultaneousSessionLimit, int totalSetLimit) {					
		
		this.simultaneousSessionLimit = simultaneousSessionLimit;
		this.totalSetLimit = totalSetLimit;
		
		// User trackers		
		idToUsername = new ConcurrentHashMap<String, String>();		
				
		// Experiment trackers
		experiments = new ConcurrentBooleanCounter<ExperimentServer<?>>();
		idToExp = new ConcurrentHashMap<String, ExperimentServer<?>>();
	}		
		
	@Override
	public abstract boolean sessionExistsInDB(String sessionID) throws SessionExpiredException;

	/**
	 * Does this worker need to take a quiz for the current set?
	 * @param workerId
	 * @return
	 */
	protected abstract boolean userRequiresQuiz(String workerId) throws TooManyFailsException;

	/* (non-Javadoc)
	 * @see edu.harvard.econcs.turkserver.server.DataTracker#sessionIsInProgress(java.math.BigInteger)
	 */
	@Override
	public final boolean sessionIsInProgress(String sessionID) {
		/* Return true if the id is assigned to an experiment and the experiment is not finished
		 * TODO fix this later to be more in sync with the DB
		 */
		return idToExp.containsKey(sessionID) && !experiments.get(idToExp.get(sessionID));
	}
	
	@Override
	public
	abstract boolean sessionCompletedInDB(String sessionID);
	
	@Override
	public abstract List<SessionRecord> getSetSessionInfoForWorker(String workerId);

	@Override
	public abstract SessionRecord getStoredSessionInfo(String sessionID);
	
	/**
	 * Associate an assignment and worker Id to a session
	 * @param sessionID
	 * @param assignmentId
	 * @param workerId
	 */
	public abstract void saveAssignmentForSession(String sessionID, 
			String assignmentId, String workerId);

	/**
	 * Save the results of a quiz
	 * @param sessionID
	 * @param workerId
	 * @param qr
	 * @throws QuizFailException 
	 */
	public abstract void saveQuizResults(String sessionID, QuizResults qr) 
	throws QuizFailException;

	/**
	 * Associate a user name to a session (only for the lobby anyway)
	 * Can also store the time this happened
	 * @param sessionId
	 * @param username
	 */
	protected abstract void saveUsernameForSession(String sessionId, String username);
		
	/* (non-Javadoc)
	 * @see edu.harvard.econcs.turkserver.server.DataTracker#saveIPForSession(java.math.BigInteger, java.net.InetAddress, java.util.Date)
	 */
	@Override
	public abstract void saveIPForSession(String id, InetAddress remoteAddress, Date lobbyTime);

	/**
	 * Saves an experiment start time in the db
	 * @param exp
	 * @param startTime
	 */
	protected abstract void saveExpStartTime(ExperimentServer<?> exp, Date startTime);
	
	/**
	 * Saves the experiment that a session started
	 * @param clientID
	 * @param experimentID
	 */
	protected abstract void saveExperimentForSession(String clientID, String experimentID);

	/**
	 * Saves an experiment end time in the db
	 * @param exp
	 * @param endTime
	 */
	protected abstract void saveExpEndTime(ExperimentServer<?> exp, Date endTime);
	
	/**
	 * Saves the amount of time (percent) a client was inactive over the entire experiment 
	 * @param sessionID
	 * @param inactivePercent
	 */
	protected abstract void saveSessionCompleteInfo(String sessionID, double inactivePercent);
	
	/**
	 * Clears the worker for a session, as well as username. 
	 * However, does not clear the assignment,
	 * which is reset when someone else connects
	 * @param id
	 */
	protected abstract void clearWorkerForSession(String id);

	/* (non-Javadoc)
	 * @see edu.harvard.econcs.turkserver.server.DataTracker#expireUnusedSessions()
	 */
	@Override
	public abstract List<SessionRecord> expireUnusedSessions();

	/* (non-Javadoc)
	 * @see edu.harvard.econcs.turkserver.server.DataTracker#registerAssignment(java.math.BigInteger, java.lang.String, java.lang.String)
	 */		
	@Override
	public final LoginStatus registerAssignment(String sessionID, String assignmentId, String workerId)
	throws ExpServerException {
		if( sessionID == null || assignmentId == null || workerId == null )
			throw new ExpServerException("Session credentials missing!");
		
		// TODO fix this to accept workers that return and accept a different HIT for the same game		 
		
		// This check is important for expired sessions!
		if( !sessionExistsInDB(sessionID) )	throw new SessionUnknownException();
		
		SessionRecord sessionRec = getStoredSessionInfo(sessionID);
		// This will not be null if this session was disconnected after an experiment
		String prevWorkerId = sessionRec.getWorkerId();

		List<SessionRecord> allSessions = getSetSessionInfoForWorker(workerId);
		
		/*
		 * Check if this person has taken too many assignments for this set
		 */
		if( allSessions != null && allSessions.size() > 0 ) {

			// Count how many sessions they have taken so far
			int currentlyAssigned = 0;			
			for( SessionRecord otherRec : allSessions ) {								
				if ( otherRec.getStatus() != SessionStatus.COMPLETED ) currentlyAssigned++;
			}				

			logger.info(
					String.format("Worker %s already has %d other sessions (%d currently assigned)",
							workerId, allSessions.size(), currentlyAssigned));
									
			/* TODO do un-assigning so we can save the fact this worker took this session anyway
			 * since right now we don't want to record them as taking a session if they get an error below
			 */
			// saveAssignmentForSession(sessionID, assignmentId, workerId);

			// Check the following if this session is a new assignment
			if( workerId.equals("A1Q7O19APQMMMG") ) {
				// TODO generalize this
				logger.info("Trapdoor for myself");
			}
			else if( !workerId.equals(prevWorkerId) ) {
				// Check sessions first before limit (games might not be yet finished)
				if( currentlyAssigned >= simultaneousSessionLimit )
					throw new SimultaneousSessionsException();	

				if( allSessions.size() >= totalSetLimit )
					throw new TooManySessionsException();	
			}
		}

		/* 
		 * Decide if this person needs to take a quiz for this set		
		 */
		if( userRequiresQuiz(workerId) ) return LoginStatus.QUIZ_REQUIRED;
		
		/* 
		 * Decide if this person needs a username for the lobby
		 */
		if( prevWorkerId != null && prevWorkerId.equals(workerId) ) {
			/* This session already had a previous assignment and 
			 * it was assigned to this worker, must be a reconnect
			 */				
			if( sessionRec.getStatus() == SessionStatus.COMPLETED ) 
				throw new SessionCompletedException();

			logger.info(String.format("Session %s (with assignment %s) by worker %s is reconnecting",
					sessionID, assignmentId, workerId));
			
			// Ask for username again if we somehow didn't get it last time
			return (sessionRec.getUsername() != null ? LoginStatus.REGISTERED : LoginStatus.NEW_USER );
		}			
		else {
			// Not reconnection from the same person
			
			if( prevWorkerId != null ) {
				// Connection was from someone else 
				
				if( sessionIsInProgress(sessionID) || sessionRec.getStatus() == SessionStatus.COMPLETED ) {
					/* TODO until we get a notification receptor, this is the place to queue up
					 * an abandoned HIT in the database 
					 * 
					 * TODO someone returned a HIT after starting 
					 * the experiment but didn't get to the submit stage
					 * We need to disable these HITs 
					 * 
					 * TODO check this by worker and possibly let someone re-take the HIT? (probably not)
					 */
					logger.info(String.format("Session %s was in experiment or completed with worker %s," +
							"but new worker %s tried to accept",
							sessionID, prevWorkerId, workerId));
					
					throw new SessionOverlapException();
				}								

				// Taking over the person in lobby
				logger.info(String.format("session %s replaced by worker %s with assignment %s",
						sessionID, workerId, assignmentId));
			}

			// First connection	for this assignment (but multiple from worker should be caught above)		
			saveAssignmentForSession(sessionID, assignmentId, workerId);
			return LoginStatus.NEW_USER;
		}

	}

	/**
	 * Sets the recorded username for a specified session ID
	 * @param sessionID
	 * @param username
	 * @return
	 */
	public final boolean lobbyLogin(String sessionID, String username) {
		if( username != null ) {
			idToUsername.put(sessionID, username);
			saveUsernameForSession(sessionID, username);
			return true;
		}		
		else {
			logger.warning(sessionID + " sent null username");
			return false;
		}
	}

	/**
	 * Record that an experiment has started with a set of clients
	 * @param newExp
	 */
	public final void newExperimentStarted(ExperimentServer<?> newExp) {
		experiments.put(newExp, false);
		
		saveExpStartTime(newExp, new Date());
		
		for( String clientID : newExp.getClients() ) {
			idToExp.put(clientID, newExp);
			saveExperimentForSession(clientID, newExp.experimentID);
		}			
	}

	public final void experimentFinished(ExperimentServer<?> exp) {
		// Doing DB commits first so there is no limbo state
		saveExpEndTime(exp, new Date());
		
		// store final client info, with inactive time measured properly for disconnections
		for( String id : exp.getClients() ) {
			saveSessionCompleteInfo(id, exp.getInactivePercent(id));
		}
		
		experiments.put(exp, true);
	}	

	/* (non-Javadoc)
	 * @see edu.harvard.econcs.turkserver.server.DataTracker#sessionDisconnected(java.math.BigInteger)
	 */
	@Override
	public final void sessionDisconnected(String id) {		
		if( idToExp.containsKey(id) ) {
			// Notify experiment server of disconnection
			idToExp.get(id).clientDisconnected(id);
		}				
				
		/* 
		 * TODO user can accept a hit then close window, but this is the same thing as 
		 * accepting multiple hits, holding and refreshing
		 * Fix with a notification receptor later.  
		 */
		if( !(sessionIsInProgress(id) || sessionCompletedInDB(id)) ) {
			/* If disconnected from lobby, clear session from worker Id list
			 * also clear the username that was stored from worker
			 * 
			 * BUT if in experiment, they need to wait	
			 */
			clearWorkerForSession(id);
			idToUsername.remove(id);
		}
		
		// TODO shouldn't we be doing something else here?
	}	

	/**
	 * Get the user name for a session
	 * User name doesn't change unless HIT returned, so store the value
	 * 
	 * @param sessionID
	 * @return garbage if the user has no name
	 */
	public final String getUsername(String sessionID) {
		if( idToUsername.containsKey(sessionID) ) 
			return idToUsername.get(sessionID); // Cached				
				
		return sessionID;
	}
	
	/**
	 * Gets the experiment that a session is in right now
	 * @param clientID
	 * @return
	 */
	public final ExperimentServer<?> getExperimentForID(String clientID) {		
		return idToExp.get(clientID);
	}
	
	/**
	 * Get the number of experiments finished in the current batch
	 * @return
	 */
	public final int getNumExpsFinished() {
		return experiments.getTrueCount();
	}

	/**
	 * Get the number of experiments running in the current batch
	 * @return
	 */
	public final int getNumExpsRunning() {		
		return experiments.getFalseCount();
	}
	
	@Override
	public int getSetLimit() {		
		return totalSetLimit;
	}	
	
}
