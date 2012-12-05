package edu.harvard.econcs.turkserver.server;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.harvard.econcs.turkserver.ExpServerException;
import edu.harvard.econcs.turkserver.QuizFactory;
import edu.harvard.econcs.turkserver.QuizMaterials;
import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.SessionCompletedException;
import edu.harvard.econcs.turkserver.SessionOverlapException;
import edu.harvard.econcs.turkserver.SimultaneousSessionsException;
import edu.harvard.econcs.turkserver.TooManyFailsException;
import edu.harvard.econcs.turkserver.TooManySessionsException;
import edu.harvard.econcs.turkserver.schema.Quiz;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

public class WorkerAuthenticator {
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

	private final ExperimentDataTracker tracker;
	
	private final QuizFactory quizFactory;
	private final QuizPolicy quizPolicy;
	
	private final List<String> specialWorkers;
	
	private final int simultaneousSessionLimit;
	private final int totalSetLimit;
	
	@Inject
	public WorkerAuthenticator(
			ExperimentDataTracker tracker,
			QuizFactory quizFactory,
			QuizPolicy quizPolicy,
			@Named("Concurrent Session Limit")
			int concurrentSessionLimit,
			@Named("Total Set Limit")
			int totalSetLimit,
			@Named("Special Workers")
			List<String> specialWorkers) {
		this.tracker = tracker;
		
		this.quizFactory = quizFactory;
		this.quizPolicy = quizPolicy;
		
		this.simultaneousSessionLimit = concurrentSessionLimit;
		this.totalSetLimit = totalSetLimit;
		
		this.specialWorkers = specialWorkers;
	}

	/**
	 * Check if a potential HIT is valid, and make sure it is in the tracker
	 * @param hitId
	 * @throws SessionCompletedException
	 */
	public void checkHITValid(String hitId, String workerId) 
			throws SessionCompletedException, SessionOverlapException  {
		Session session = null;
		
		if( !tracker.hitExistsInDB(hitId) ) {
			tracker.saveHITId(hitId);
			
			/* This has been temporarily replaced to re-use old HITs */			
		}
		else if( (session = tracker.getStoredSessionInfo(hitId)) != null &&
				SessionRecord.status(session) == SessionStatus.COMPLETED ) {
			
			if( workerId.equals(session.getWorkerId()) ) {
				throw new SessionCompletedException();	
			} else {
				// Prevent bugs from old, completed (but not submitted properly) 
				// still-circulating HITs from being reused			
				throw new SessionOverlapException();
			}						
		}
	}
	
	/**
	 * Check if a worker attempting to take a HIT is exceeding any session or total limits
	 * 
	 * @param hitId
	 * @param assignmentId
	 * @param workerId
	 */
	public void checkWorkerLimits(String hitId, String assignmentId, String workerId)
	throws SimultaneousSessionsException, TooManySessionsException {
		// TODO fix this to accept workers that return and accept a different HIT for the same game		 			
		
		Session sessionRec = tracker.getStoredSessionInfo(hitId);		
		/* This could be null if no other worker has taken the HIT yet 
		 * will not be null if this session was disconnected after an experiment		
		 */
		String prevWorkerId = sessionRec.getWorkerId();

		List<Session> allSessions = tracker.getSetSessionInfoForWorker(workerId);

		// Return if no sessions so far in this set
		if( allSessions == null || allSessions.size() == 0 ) return;		
		
		// Count how many sessions they have taken so far
		int currentlyAssigned = 0;			
		for( Session otherRec : allSessions ) {								
			if ( SessionRecord.status(otherRec) != SessionStatus.COMPLETED ) currentlyAssigned++;
		}				

		logger.info(
				String.format("Worker %s already has %d other sessions (%d currently assigned)",
						workerId, allSessions.size(), currentlyAssigned));

		/* TODO do un-assigning so we can save the fact this worker took this session anyway
		 * since right now we don't want to record them as taking a session if they get an error below
		 */
		// saveAssignmentForSession(sessionID, assignmentId, workerId);

		// Check the following if this session is a new assignment
		if( specialWorkers.contains(workerId) ) {
			logger.info("Ignored limits for special worker " + workerId);
		}
		else if( !workerId.equals(prevWorkerId) ) {
			// Check sessions first before limit (games might not be yet finished)
			if( currentlyAssigned >= simultaneousSessionLimit )
				throw new SimultaneousSessionsException();	

			if( allSessions.size() >= totalSetLimit )
				throw new TooManySessionsException();	
		}
		
	}
	
	/**
	 * Does this worker need to take a quiz for the current set?
	 * @param workerId
	 * @return
	 */
	public boolean workerRequiresQuiz(String workerId) throws TooManyFailsException {				
		if( quizPolicy == null ) return false;
		
		List<Quiz> results = tracker.getSetQuizRecords(workerId);
				
		if( !quizPolicy.requiresQuiz(results) ) return false;
		
		// See if we should let them have another try
		if( quizPolicy.overallFail(results) ) 
			throw new TooManyFailsException();		
		
		return true;
		
	}

	/**
	 * Checks the login information for this user. 
	 * 
	 * TODO fix this to check if a quiz is required for someone connecting
	 * 
	 * @param hitId
	 * @param assignmentId
	 * @param workerId 
	 * @throws ExpServerException
	 */	
	public Session checkAssignment(String hitId, String assignmentId, String workerId)
	throws ExpServerException {

		Session sessionRec = tracker.getStoredSessionInfo(hitId);		
		// previous worker could be null
		String prevWorkerId = sessionRec.getWorkerId();		
				
		if( workerId.equals(prevWorkerId) ) {
			/* This session already had a previous assignment and 
			 * it was assigned to this worker, must be a reconnect
			 */				
			if( SessionRecord.status(sessionRec) == SessionStatus.COMPLETED ) 
				throw new SessionCompletedException();

			logger.info(String.format("Session %s (with assignment %s) by worker %s is reconnecting",
					hitId, assignmentId, workerId));
						
			return sessionRec;
		}			
		
		/* 
		 * Not reconnection from the same person
		 */
		if( prevWorkerId != null ) {
			// Connection was from someone else 

			if( SessionRecord.status(sessionRec) == SessionStatus.EXPERIMENT || 
					SessionRecord.status(sessionRec) == SessionStatus.COMPLETED ) {
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
						hitId, prevWorkerId, workerId));

				throw new SessionOverlapException();
			}								

			// Taking over the person in lobby
			logger.info(String.format("session %s replaced by worker %s with assignment %s",
					hitId, workerId, assignmentId));
		}

		// First connection	for this assignment (but multiple from worker should be caught above)		
		tracker.saveAssignmentForSession(hitId, assignmentId, workerId);

		sessionRec.setAssignmentId(assignmentId);
		sessionRec.setWorkerId(workerId);
		return sessionRec;		
	}
	
	public boolean quizPasses(QuizResults qr) {
		return quizPolicy.quizPasses(qr);
	}
	
	public QuizMaterials getQuiz() {		
		return quizFactory.getQuiz();
	}

	public int getConcurrentLimit() {
		return simultaneousSessionLimit;
	}
	
	/**
	 * Gets the maximum number of allowed tasks per set
	 * @return
	 */	
	public int getSetLimit() {		
		return totalSetLimit;
	}
	
	
}
