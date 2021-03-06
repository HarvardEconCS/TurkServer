/**
 * 
 */
package edu.harvard.econcs.turkserver.server.mysql;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import net.andrewmao.misc.ConcurrentBooleanCounter;

import edu.harvard.econcs.turkserver.ExpServerException;
import edu.harvard.econcs.turkserver.SessionCompletedException;
import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.SessionOverlapException;
import edu.harvard.econcs.turkserver.SessionUnknownException;
import edu.harvard.econcs.turkserver.SimultaneousSessionsException;
import edu.harvard.econcs.turkserver.TooManySessionsException;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;

/**
 * deprecated in favor of single data tracker.
 * kept here as a reference of authentication method.
 * 
 * @author mao
 *
 */
@Deprecated
public abstract class SimpleDataTracker {
	
	protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());	
	
	private final int simultaneousSessionLimit;
	private final int totalSetLimit;

	// False for in-progress sessions and True for completed 
	private ConcurrentBooleanCounter<String> sessionStatus;
	
	protected SimpleDataTracker(int simultaneousSessionLimit, int totalSetLimit) {
		this.simultaneousSessionLimit = simultaneousSessionLimit;
		this.totalSetLimit = totalSetLimit;
	}		

	public int getSetLimit() {
		return totalSetLimit;
	}
	
	
	public boolean sessionIsInProgress(String hitID) {
		Boolean status = sessionStatus.get(hitID);
		return status != null && status == false;
	}

	public abstract List<Object[]> getWorkerAndTotalDataCounts(String workerId);	

	public abstract void setSessionData(String hitId, String Data);

	public abstract void saveSessionLog(String hitId, String data);

	
	public final void registerAssignment(String hitID,
			String assignmentId, String workerId) throws ExpServerException {
		
		if( !sessionExistsInDB(hitID) )	{
			// This check is important for old sessions!
			// re-registers this session for this set
			this.saveHITId(hitID);			
		}
		else if( sessionCompletedInDB(hitID) ) {
			// Prevent bugs from old, completed but 
			// still-circulating HITs from being reused
			// TODO delete this, because sessionView was probably causing this bug
			throw new SessionUnknownException();
		}

		Session sessionRec = getStoredSessionInfo(hitID);
		// This will not be null if this session was disconnected after already accepting
		String prevWorkerId = sessionRec.getWorkerId();

		List<Session> allWorkerSessions = getSetSessionInfoForWorker(workerId);

		/*
		 * Check if this person has taken too many assignments for this set
		 */
		if( allWorkerSessions != null && allWorkerSessions.size() > 0 ) {
			// Count how many sessions they have taken so far
			int currentlyAssigned = 0;			
			for( Session otherRec : allWorkerSessions ) {								
				if ( SessionRecord.status(otherRec) != SessionStatus.COMPLETED ) 
					currentlyAssigned++;
			}				

			logger.info(
					String.format("Worker %s already has %d other sessions (%d currently assigned)",
							workerId, allWorkerSessions.size(), currentlyAssigned));

			// Check the following if this session is a new assignment
			if( !workerId.equals(prevWorkerId) ) {
				// Check sessions first before limit (games might not be yet finished)
				if( currentlyAssigned >= simultaneousSessionLimit )
					throw new SimultaneousSessionsException();	

				if( allWorkerSessions.size() >= totalSetLimit )
					throw new TooManySessionsException();	
			}
		}
		
		/* 
		 * Decide if this is a new worker for this session or not
		 */
		if( prevWorkerId != null && prevWorkerId.equals(workerId) ) {
			/* This session already had a previous assignment and 
			 * it was assigned to this worker, must be a reconnect
			 */				
			if( SessionRecord.status(sessionRec) == SessionStatus.COMPLETED ) 
				throw new SessionCompletedException();

			logger.info(String.format("HIT %s (with assignment %s) by worker %s is reconnecting",
					hitID, assignmentId, workerId));
			
			
			return; // LoginStatus.REGISTERED;
		}			
		else {
			// Not reconnection from the same person

			if( prevWorkerId != null ) {
				// Connection was from someone else 

				if( SessionRecord.status(sessionRec) == SessionStatus.COMPLETED ) {
					// TODO we can probably just re-appropriate the HIT here

					logger.info(String.format("HIT %s was completed with worker %s," +
							"but new worker %s tried to accept",
							hitID, prevWorkerId, workerId));

					throw new SessionOverlapException();
				}
				
//				else if ( sessionIsInProgress(sessionID) ) {
//					// Just reassign this to the new worker					
//				}

				// Taking over the person in lobby
				logger.info(String.format("HIT %s replaced by worker %s with assignment %s",
						hitID, workerId, assignmentId));
			}

			// First connection	for this assignment (but multiple from worker should be caught above)		
			saveAssignmentForSession(hitID, assignmentId, workerId);
			return; // LoginStatus.NEW_USER;
		}
	}

	
	protected abstract void saveAssignmentForSession(String hitID, String assignmentId,
			String workerId);

	protected abstract List<Session> getSetSessionInfoForWorker(String workerId);

	protected abstract Session getStoredSessionInfo(String hitID);

	protected abstract boolean sessionCompletedInDB(String hitID);

	protected abstract void saveHITId(String hitID);

	protected abstract boolean sessionExistsInDB(String hitID) throws SessionExpiredException;

	public abstract void saveIPForSession(String hitID, InetAddress remoteAddress, Date lobbyTime);

	
	public final void sessionDisconnected(String hitID) {											
		// Don't need to do anything here, new worker will be automatically replaced when joining
	}

	
	public abstract List<Session> expireUnusedSessions();

}
