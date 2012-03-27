package edu.harvard.econcs.turkserver.server.mysql;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;

import edu.harvard.econcs.turkserver.ExpServerException;
import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.Codec.LoginStatus;
import edu.harvard.econcs.turkserver.server.SessionRecord;

public interface DataTracker<T> {

	/**
	 * Get a new, unused session ID, unique in the database
	 * @param len
	 * @param rnd
	 * @return
	 */
	public abstract T getNewSessionID();

	/**
	 * Check if we have a record of a sessionID in the database,
	 * including from other sets
	 * @param sessionID
	 * @return
	 */
	boolean sessionExistsInDB(T sessionID) throws SessionExpiredException;
	
	/**
	 * Check if a session is assigned and not yet submitted
	 * @param sessionID
	 * @return
	 */
	public abstract boolean sessionIsInProgress(T sessionID);

	/**
	 * Check if a session is completed in the database
	 * @param sessionID
	 * @return
	 */
	abstract boolean sessionCompletedInDB(T sessionID);
	
	/**
	 * Returns a list of all sessions in the current experiment set 
	 * associated with a worker that have had experiments completed 
	 * @param workerId
	 * @return
	 */
	abstract List<SessionRecord> getSetSessionInfoForWorker(String workerId);

	/**
	 * Get the assignment Id, if any, for a session
	 * @param sessionID
	 * @return
	 */
	abstract SessionRecord getStoredSessionInfo(T sessionID);
	
	/**
	 * Associate a hit ID to a session ID
	 * @param sessionID
	 * @param hitId
	 */
	public abstract void saveHITIdForSession(T sessionID, String hitId);

	/**
	 * Associate an assignment and worker Id to a session
	 * @param sessionID
	 * @param assignmentId
	 * @param workerId
	 */
	public abstract void saveAssignmentForSession(T sessionID, 
			String assignmentId, String workerId);
	
	/**
	 * Checks the login information for this user. 
	 * This is called by a worker thread via RMI so it's probably fine if 
	 * it does a bunch of DB lookups.
	 * 
	 * TODO fix this to check if a quiz is required for someone connecting
	 * 
	 * @param sessionID
	 * @param assignmentId
	 * @param workerId 
	 * @return true if this user is reconnecting
	 * @throws ExpServerException
	 */
	public abstract LoginStatus registerAssignment(T sessionID,
			String assignmentId, String workerId) throws ExpServerException;

	/**
	 * Saves the IP address for a given session
	 * @param id
	 * @param remoteSocketAddress
	 */
	public abstract void saveIPForSession(T id,
			InetAddress remoteAddress, Date lobbyTime);

	/**
	 * Called when the user with this ID disconnects, so appropriate cleanup can be done
	 * @param id
	 */
	public abstract void sessionDisconnected(T id);

	/**
	 * Sets all sessions with NULL experiment in the DB to expired, and returns the HIT IDs
	 * @return
	 */
	public abstract List<SessionRecord> expireUnusedSessions();

	/**
	 * Gets the maximum number of allowed tasks per session
	 * @return
	 */
	public abstract int getSetLimit();

}