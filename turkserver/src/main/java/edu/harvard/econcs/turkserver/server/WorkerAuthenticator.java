package edu.harvard.econcs.turkserver.server;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.harvard.econcs.turkserver.QuizMaterials;
import edu.harvard.econcs.turkserver.SessionCompletedException;
import edu.harvard.econcs.turkserver.SessionOverlapException;
import edu.harvard.econcs.turkserver.SimultaneousSessionsException;
import edu.harvard.econcs.turkserver.TooManyFailsException;
import edu.harvard.econcs.turkserver.TooManySessionsException;
import edu.harvard.econcs.turkserver.config.TSConfig;
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
			@Nullable 
			QuizFactory quizFactory,
			@Nullable 
			QuizPolicy quizPolicy,
			Configuration conf,
			@Named(TSConfig.EXP_SPECIAL_WORKERS)
			List<String> specialWorkers) {
		this.tracker = tracker;
		
		this.quizFactory = quizFactory;
		this.quizPolicy = quizPolicy;
		
		this.simultaneousSessionLimit = conf.getInt(TSConfig.CONCURRENCY_LIMIT);
		this.totalSetLimit = conf.getInt(TSConfig.EXP_REPEAT_LIMIT);
		
		this.specialWorkers = specialWorkers;
	}

	/**
	 * Check if a potential HIT is valid, and make sure it is in the tracker
	 * @param hitId
	 * @throws SessionCompletedException
	 */
	public void checkHITValid(String hitId, String workerId, Session existingRecord) 
			throws SessionCompletedException, SessionOverlapException  {
		
		if( existingRecord == null ) {
			existingRecord = tracker.getStoredSessionInfo(hitId);
		}
		
		if( existingRecord == null ) {
			tracker.saveHITId(hitId);								
		}
		else {
			// Check for pathologies with previously used HITs
			
			if( workerId.equals(existingRecord.getWorkerId()) ) { 							
				if( SessionRecord.status(existingRecord) == SessionStatus.COMPLETED ) { 
					/* This session already had a previous assignment and 
					 * it was assigned to this worker, must be a reconnect
					 */					
					throw new SessionCompletedException();
				}
				else {
					logger.info("Session {} by worker {} is reconnecting", hitId, workerId);
				}
			} 			
			else if( !workerId.equals(existingRecord.getWorkerId()) && 
					( SessionRecord.status(existingRecord) == SessionStatus.COMPLETED || 
					SessionRecord.status(existingRecord) == SessionStatus.EXPERIMENT ) ) {
				// Prevent bugs from old, completed (but not submitted properly) 
				// still-circulating HITs from being reused
								
				/* TODO until we get a notification receptor, this is the place to queue up
				 * an abandoned HIT in the database 
				 * 
				 * TODO someone returned a HIT after starting the experiment but didn't get to 
				 * the submit stage.  We need to disable these HITs 
				 * 
				 * TODO check this by worker and possibly let someone re-take the HIT? (probably not)
				 */
				logger.info(String.format("Session %s was in experiment or completed with worker %s," +
						"but new worker %s tried to accept",
						hitId, existingRecord.getWorkerId(), workerId));
				
				throw new SessionOverlapException();
			}						
		}
	}
	
	/**
	 * Check if a worker attempting to take a HIT is exceeding any session or total limits
	 * 
	 * TODO fix this to accept workers that return and accept a different HIT for the same game
	 *  
	 * @param hitId
	 * @param assignmentId
	 * @param workerId
	 */
	public void checkWorkerLimits(String hitId, String workerId, Session existingRecord)
	throws SimultaneousSessionsException, TooManySessionsException {		
		if (existingRecord == null)	existingRecord = tracker.getStoredSessionInfo(hitId);
		
		/* This could be null if no other worker has taken the HIT yet 
		 * will not be null if this session was disconnected after an experiment		
		 */
		String prevWorkerId = existingRecord == null ? null : existingRecord.getWorkerId();

		Collection<Session> allSessions = tracker.getSetSessionInfoForWorker(workerId);

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
		
		Collection<Quiz> results = tracker.getSetQuizRecords(workerId);
				
		if( !quizPolicy.requiresQuiz(results) ) return false;
		
		// See if we should let them have another try
		if( quizPolicy.overallFail(results) ) 
			throw new TooManyFailsException();		
		
		return true;		
	}

	/**
	 * After taking a quiz (and failing), should this worker be locked out?
	 * @param workerId
	 * @return
	 */
	public boolean tooManyFails(String workerId) {
		Collection<Quiz> results = tracker.getSetQuizRecords(workerId);
		
		return quizPolicy.overallFail(results);
	}
	
	public boolean quizPasses(Quiz qr) {
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
