package edu.harvard.econcs.turkserver.server;

import java.net.InetAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.BayeuxServer.SessionListener;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.JettyJSONContextServer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ajax.JSON.Convertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.inject.Inject;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.Messages;
import edu.harvard.econcs.turkserver.QuizMaterials;
import edu.harvard.econcs.turkserver.SessionCompletedException;
import edu.harvard.econcs.turkserver.SessionOverlapException;
import edu.harvard.econcs.turkserver.SimultaneousSessionsException;
import edu.harvard.econcs.turkserver.TooManyFailsException;
import edu.harvard.econcs.turkserver.TooManySessionsException;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.schema.Quiz;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker.SessionSummary;

public abstract class SessionServer extends Thread {

	public static final String ATTRIBUTE = "edu.harvard.econcs.turkserver.sessions";

	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
			
	final ExperimentDataTracker tracker;
	final HITController hitCont;
	final WorkerAuthenticator workerAuth;	
	final Experiments experiments;
	
	protected final boolean debugMode;
	protected final int hitGoal;		
	
	protected JettyCometD jettyCometD;
	
	protected BayeuxServer bayeux;	
	
	protected ConcurrentMap<ServerSession, HITWorkerImpl> clientToHITWorker;
	protected Table<String, String, HITWorkerImpl> hitWorkerTable;
	
	protected volatile int completedHITs = 0;
	protected volatile int submittedHITs = 0;
	
	private volatile boolean running = true;
	
	@Inject
	public SessionServer(			
			ExperimentDataTracker tracker,			
			HITController hitCont,
			WorkerAuthenticator workerAuth,
			Experiments experiments,			
			Configuration config
			) throws ClassNotFoundException {		
		
		this.tracker = tracker;								
		this.hitCont = hitCont;
		this.workerAuth = workerAuth;
		this.experiments = experiments;		
		
		/*
		 * Process configuration
		 */
				
		this.debugMode = config.getBoolean(TSConfig.SERVER_DEBUGMODE);
		this.hitGoal = config.getInt(TSConfig.SERVER_HITGOAL);					
		
		/*
		 * create session and string maps
		 */
		final MapMaker mm = new MapMaker();
		this.clientToHITWorker = mm.makeMap();		
				
		Map<String, Map<String, HITWorkerImpl>> rowMap = mm.makeMap();
		mm.concurrencyLevel(1);
		this.hitWorkerTable = Tables.newCustomTable(
				rowMap, new Supplier<Map<String, HITWorkerImpl>>() {
					@Override public Map<String, HITWorkerImpl> get() { return mm.makeMap(); }					
				});										  
	}
	
	public void injectWebServer(JettyCometD server) {		
		this.jettyCometD = server;
	}

	public class UserSessionListener implements SessionListener {
		@Override
		public void sessionAdded(ServerSession conn) {
			HITWorkerImpl previous = clientToHITWorker.get(conn);			
			
			if( previous != null ) {
				// This session was previously connected. Associate it with the HITWorker. 
				previous.setServerSession(conn);
				logger.info(conn.getId() + " reconnected, used to have HIT " + previous.getHitId());
			}
			else {
				logger.info(conn.toString());
//				clientToHITWorker.putIfAbsent(session, null);
			}
		}

		@Override
		public void sessionRemoved(ServerSession conn, boolean timedout) {			
			String clientId = conn.getId();			
			
			if( timedout ) {
				logger.info("Session " + clientId + " timed out");
				sessionDisconnect(conn);
			}
			else {
				logger.info("Session " + clientId + " disconnected");
				sessionDisconnect(conn);
			}
		}	
	}

	public void registerConvertor(Class<?> cl, Convertor conv) {
		// TODO put this elsewhere
		JettyJSONContextServer jsonContext = (JettyJSONContextServer) bayeux.getOption("jsonContext");
		jsonContext.getJSON().addConvertor(cl, conv);
	}
	
	public abstract int getExpsInProgress();

	public abstract int getExpsCompleted();
	
	public int getNumCompleted() {		
		return completedHITs;
	}

	void sessionView(ServerSession conn, String hitId) {
		if( hitId != null ) {					
			
			// This session currently hasn't accepted this HIT 
			HITWorkerImpl prev = clientToHITWorker.remove(conn);
						
			logger.info("New user is viewing HIT {}, previous user was {}", hitId, prev);
			
			conn.setAttribute("hitId", hitId);
			
			// Try to add hitIds that we have no record of (clean reuse)
			if( !tracker.hitExistsInDB(hitId) )
				tracker.saveHITId(hitId);			
		}
		else {
			System.out.println("Client " + conn.getId() + " sent null hitId");
		}
	}

	HITWorkerImpl sessionAccept(ServerSession conn, 
			String hitId, String assignmentId, String workerId) {				
		if( hitId == null ) {
			logger.warn("Received null hitId for session {}", conn.getId());
			return null;
		}	
		
		Session hitIdRecord = tracker.getStoredSessionInfo(hitId);
		
		try {
			workerAuth.checkHITValid(hitId, workerId, hitIdRecord);
			
			// Create hitId record that would have been saved above if session was null
			if( hitIdRecord == null ) {				
				hitIdRecord = new Session();
				hitIdRecord.setHitId(hitId);
			}
		}
		catch (SessionCompletedException e) {
			SessionUtils.sendStatus(conn, Codec.status_expfinished, Messages.SESSION_COMPLETED);
			logger.info("Worker {} connected to experiment after completion (HIT {})", workerId, hitId);
			
			// Re-match HITWorker to this person
			HITWorkerImpl hitw;
			if( (hitw = hitWorkerTable.get(hitId, workerId)) != null ) {
				// Match session to HITWorker and vice versa
				ServerSession oldSession = hitw.cometdSession.get();
				if( oldSession == null || !conn.equals(oldSession) ) {
					hitw.setServerSession(conn);				
				}
				clientToHITWorker.put(conn, hitw);
			}								
			return null;
		} catch (SessionOverlapException e) {
			SessionUtils.sendStatus(conn, Codec.status_sessionoverlap, Messages.SESSION_OVERLAP);
			logger.info("Worker {} connected to overlapping session (HIT {})", workerId, hitId);
			
			/*
			 * Disable this HIT and prevent recurring problems
			 * We can do this synchronously in this thread
			 */
			logger.info("Disabling overlapping HIT {} as it was picked up by someone else", hitId);
			hitCont.disableHIT(hitId);
			
			return null;
		}
		
		// Check for this AFTER possible completed sessions
		if( completedHITs >= hitGoal ) {
			SessionUtils.sendStatus(conn, Codec.status_batchfinished, Messages.BATCH_COMPLETED);
			logger.info("Ignoring connection after quota reached (HIT {})", hitId);
			return null;
		}	
		
		try {
			workerAuth.checkWorkerLimits(hitId, workerId, hitIdRecord);
		} catch (SimultaneousSessionsException e) {			
			SessionUtils.sendStatus(conn, Codec.status_simultaneoussessions, Messages.SIMULTANEOUS_SESSIONS);
			logger.info("Worker {} has accepted too many HITs (HIT {})", workerId, hitId);
			return null;
		} catch (TooManySessionsException e) {			
			SessionUtils.sendStatus(conn, Codec.status_toomanysessions, Messages.TOO_MANY_SESSIONS);
			logger.info("Worker {} has completed too many HITs (HIT {})", workerId, hitId);
			return null;
		}		
		
		// Save some extra information that we can access later, for comparison 					
		conn.setAttribute("hitId", hitId);
		conn.setAttribute("assignmentId", assignmentId);
		conn.setAttribute("workerId", workerId);				
		
		try {
			if( workerAuth.workerRequiresQuiz(workerId) ) {		
				logger.info("Worker {} needs to take quiz (HIT {})", workerId, hitId);
				QuizMaterials qm = workerAuth.getQuiz();
								
				// TODO: null quiz is passed for static client-side 											
				Map<String, Object> data = qm == null ?
						ImmutableMap.of("status", (Object) Codec.status_quizneeded) :
							ImmutableMap.of("status", Codec.status_quizneeded,	"quiz", qm.toData() );
				SessionUtils.sendServiceMsg(conn, data);									
								
				return null;
			}
			
			logger.info("No quiz required for {}", workerId);			
		} catch (TooManyFailsException e) {
			SessionUtils.sendStatus(conn, Codec.status_failsauce, Messages.TOO_MANY_FAILS); 		
			logger.info("Worker {} has failed quiz too many times (HIT {})", workerId, hitId);
			return null;
		}
		
		HITWorkerImpl hitw = null;
		
		/* 
		 * Not reconnection from the same person
		 * Connection was from someone else 
		 */
		if( hitIdRecord.getWorkerId() != null && !workerId.equals(hitIdRecord.getWorkerId()) ) {										
			logger.info(String.format("HIT %s being replaced by worker %s with assignment %s", hitId, workerId, assignmentId));
		}		else {
			logger.info(String.format("HIT %s newly assigned to worker %s with assignment %s", hitId, workerId, assignmentId));
		}
				
		// Find this HITWorker in table
		if( (hitw = hitWorkerTable.get(hitId, workerId)) == null ) {
			// create instance of HITWorker			
			hitw = new HITWorkerImpl(conn, hitIdRecord);
			hitWorkerTable.put(hitId, workerId, hitw);
		}
			
		// Match session to HITWorker and vice versa
		ServerSession oldSession = hitw.cometdSession.get();
		if( oldSession == null || !conn.equals(oldSession) ) {
			hitw.setServerSession(conn);				
		}			
		clientToHITWorker.put(conn, hitw);

		tracker.saveWorkerAssignment(hitw, assignmentId, workerId);
		
		// Successful registration, save info
		tracker.saveIP(hitw, bayeux == null ? // TODO mock this part for unit tests
				InetAddress.getLoopbackAddress() : 
					bayeux.getContext().getRemoteAddress().getAddress(), new Date());			
		
		// subclasses continue with additional logic if necessary 		
		return hitw;
	}

	void sessionSubmit(ServerSession conn, String survey) {
		HITWorkerImpl worker = clientToHITWorker.get(conn);
		
		if( worker == null ) {
			logger.warn("Unrecognized client {} tried to submit", conn.getId());
			return;
		}
		
		// Write any exit comments / logs for session		 
		Session existing = worker.getSessionRecord();			
		boolean alreadySubmitted = existing.getComment() != null;						
		
		if( !alreadySubmitted ) {
			// We do this because the first survey is probably going to be better than subsequent ones
			tracker.saveExitSurveyResults(worker, survey);
			updateCompletion();
		}				
		
		// TODO check the total number of possible different tasks as well, from assigner									
		int additional = workerAuth.getSetLimit() - tracker.getSetSessionInfoForWorker(worker.getWorkerId()).size();		
		SessionUtils.sendStatus(conn, Codec.status_completed, 
				"Thanks for your work! You may do " + additional + " more HITs in this session.");		
	}

	/**
	 * Reconnect a user to an experiment, counting their disconnection time
	 * @param conn
	 * @param hitw
	 */
	void sessionReconnect(ServerSession conn, HITWorkerImpl hitw) {			
		// experiment should send state to user with this callback
		experiments.workerReconnected(hitw);				
	}
	
	void sessionDisconnect(ServerSession conn) {
		HITWorkerImpl worker = clientToHITWorker.get(conn);
		
		if( worker != null ) {
			experiments.workerDisconnected(worker);			
		}
		
		String workerHitId = worker == null ? null : worker.getHitId(); 				
		if( workerHitId != null ) {
			/* 
			 * TODO user can accept a hit then close window, but this is the same thing as 
			 * accepting multiple hits, holding and refreshing
			 * Fix with a notification receptor later.  
			 */
			if( !(experiments.workerIsInProgress(worker) || 
					SessionRecord.status(worker.record) == SessionStatus.EXPERIMENT ||
					SessionRecord.status(worker.record) == SessionStatus.COMPLETED) ) {
				/* If disconnected from lobby or earlier, clear session from worker Id list
				 * also clear the username that was stored from worker
				 * 
				 * BUT if in experiment, they need to wait	
				 */
				tracker.clearWorkerForSession(workerHitId);
			}
		}
		
		// Debug check for any strange inconsistencies
		String sessionHitId = (String) conn.getAttribute("hitId");		
		if( workerHitId != null && sessionHitId != null	&& !workerHitId.equals(sessionHitId) ) {
			logger.error("Session and worker HIT IDs don't match for {}", conn.getId());
		}
		
	}
	
	void rcvQuizResults(ServerSession conn, Quiz qr) {
		String workerId = (String) conn.getAttribute("workerId");
		String hitId = (String) conn.getAttribute("hitId");
		String assignmentId = (String) conn.getAttribute("assignmentId");
		
		if( workerId == null ) {
			logger.error("Can't save quiz: unknown worker for {}", conn.getId());
		}
		
		tracker.saveQuizResults(hitId, workerId, qr);

		// check if quiz failed
		if( workerAuth.quizPasses(qr) ) {
			logger.info("{} passed quiz", workerId);
			// Run the accept HIT checks again		
			sessionAccept(conn, hitId, assignmentId, workerId);
		}
		else {
			if( workerAuth.tooManyFails(workerId) ) {
				logger.info("{} failed quiz too many times", workerId);
				SessionUtils.sendStatus(conn, Codec.status_failsauce);
			}
			else {
				logger.info("{} failed quiz", workerId);
				SessionUtils.sendStatus(conn, Codec.status_quizfail);
			}
		}		
	}

	void rcvInactiveTime(ServerSession conn, long inactiveStart, long inactiveTime) {
		HITWorkerImpl worker = clientToHITWorker.get(conn);
		
		if( worker == null ) {
			logger.error("Can't save inactivity: unknown worker for {}", conn.getId());
			return;
		}		
		
		worker.addInactiveTime(inactiveStart, inactiveTime);
	}

	void rcvExperimentServiceMsg(ServerSession conn,
			Map<String, Object> dataAsMap) {
		HITWorkerImpl worker = clientToHITWorker.get(conn);
		
		if( worker != null ) experiments.rcvServiceMsg(worker, dataAsMap);
		else logger.warn("Message from unrecognized client: {}", conn.getId());
	}

	boolean rcvExperimentBroadcastMsg(ServerSession conn,
			Map<String, Object> dataAsMap) {
		HITWorkerImpl worker = clientToHITWorker.get(conn);
		
		if( worker != null ) return experiments.rcvBroadcastMsg(worker, dataAsMap);
		else logger.warn("Message from unrecognized client: {}", conn.getId());
		
		return false;
	}

	/**
	 * A group of workers has completed their tasks.
	 * @param group
	 * @return whether the server should shut down
	 */
	boolean groupCompleted(HITWorkerGroup group) {
		updateCompletion(); // Will interrupt if necesasry
		
		return completedHITs >= hitGoal;
	}

	void updateCompletion() {
		SessionSummary currentState = tracker.getSetSessionSummary();
		
		completedHITs = currentState.completedHITs;
		submittedHITs = currentState.submittedHITs;
		
		logger.info(currentState.completedHITs + " HITs completed");
		logger.info(currentState.submittedHITs + " HITs submitted");
		
		if( completedHITs >= hitGoal && completedHITs == submittedHITs ) 
			this.interrupt();
	}

	@Override
	public final void run() {
		Thread.currentThread().setName(this.getClass().getSimpleName());				
		
		Server server = null;
		try {
			server = jettyCometD.start(this);
		} catch (Exception e) {			
			e.printStackTrace();
			return;
		}
		
		bayeux = jettyCometD.getBayeux();		
		experiments.setReferences(bayeux);
		
		Thread hcThread = null;
		if( hitCont != null ) {
			(hcThread = new Thread(hitCont)).start();
		}
		
		runServerInit();
		
		// TODO: clean up half-baked experiments in database
		updateCompletion();
		
	    // Hang out until goal # of HITs are reached and shutdown jetty server
		while( running && completedHITs < hitGoal ) {			
			try { Thread.sleep(5000); }
			catch (InterruptedException e) {}
		}
				
		logger.info("Goal reached or shut down initiated - deleting remaining HITs");
		
		if( hitCont != null ) {
			hitCont.disableAndShutdown();
			try {
				hcThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}				
		
		/*
		 * Sleep until submitted HITs to equal completed HITs
		 * This may be less than hitGoal
		 */
		if( !debugMode ) {
			logger.info("Waiting for workers to submit HITs");
			do try {
				Thread.sleep(5000);
			} catch(InterruptedException e ) {} 
			while( completedHITs > submittedHITs );
			logger.info("Got all HIT submissions");
		}
		else {						
			try { Thread.sleep(1000); }
			catch (Exception e ) { e.printStackTrace();	}
		}

		// Stop experiments thread
		ScheduledExecutorService exec = experiments.stop();			
		System.out.println("Waiting for experiment scheduler to stop...");
		do try {						
			exec.awaitTermination(1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {			
			e.printStackTrace();
		} while( !exec.isTerminated() );	
		
		System.out.println("Shutting down jetty server");		
		try {			
			server.stop();
			server.join();
		} catch (InterruptedException e) {			
			e.printStackTrace();
		} catch (Exception e) {			
			e.printStackTrace();
		}
		
	}

	public void shutdown() {
		running = false;
		
		// TODO add stuff from above to in here, like auto expiring hits		
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			public void run() {				
//				logger.info("Shutdown initiated");								
//			}
//		});
		
		this.interrupt();
	}

	protected abstract void runServerInit();

}