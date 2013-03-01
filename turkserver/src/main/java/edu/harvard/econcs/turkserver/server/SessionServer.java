package edu.harvard.econcs.turkserver.server;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.BayeuxServer.SessionListener;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.JettyJSONContextServer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ajax.JSON.Convertor;
import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.inject.Inject;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.ExpServerException;
import edu.harvard.econcs.turkserver.Messages;
import edu.harvard.econcs.turkserver.QuizMaterials;
import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.SessionCompletedException;
import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.SessionOverlapException;
import edu.harvard.econcs.turkserver.SimultaneousSessionsException;
import edu.harvard.econcs.turkserver.TooManyFailsException;
import edu.harvard.econcs.turkserver.TooManySessionsException;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

public abstract class SessionServer extends Thread {

	public static final String ATTRIBUTE = "edu.harvard.econcs.turkserver.sessions";

	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
			
	final ExperimentDataTracker tracker;
	final HITController hitCont;
	final WorkerAuthenticator workerAuth;	
	final Experiments experiments;
	
	protected final boolean debugMode;
	protected final int hitGoal;		
	
	protected final JettyCometD jettyCometD;
	
	protected BayeuxServer bayeux;	
	
	protected ConcurrentMap<ServerSession, HITWorkerImpl> clientToHITWorker;
	protected Table<String, String, HITWorkerImpl> hitWorkerTable;
	
	protected final AtomicInteger completedHITs;	
	private volatile boolean running = true;
	
	@Inject
	public SessionServer(			
			ExperimentDataTracker tracker,			
			HITController hitCont,
			WorkerAuthenticator workerAuth,
			Experiments experiments,
			JettyCometD server,
			Configuration config
			) throws ClassNotFoundException {		
		
		this.tracker = tracker;								
		this.hitCont = hitCont;
		this.workerAuth = workerAuth;
		this.experiments = experiments;
		
		this.jettyCometD = server;
		
		this.completedHITs = new AtomicInteger();
		
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

	public class UserSessionListener implements SessionListener {
		@Override
		public void sessionAdded(ServerSession session) {
			HITWorkerImpl previous = clientToHITWorker.get(session);			
			
			if( previous != null ) {
				// This session was previously connected. Associate it with the HITWorker. 
				previous.setServerSession(session);
				logger.info(session.getId() + " reconnected, used to have HIT " + previous.getHitId());
			}
			else {
				logger.info(session.toString());
//				clientToHITWorker.putIfAbsent(session, null);
			}
		}

		@Override
		public void sessionRemoved(ServerSession session, boolean timedout) {			
			String clientId = session.getId();			
			
			if( timedout ) {
				Log.getRootLogger().info("Session " + clientId + " timed out");
				sessionDisconnect(session);
			}
			else {
				Log.getRootLogger().info("Session " + clientId + " disconnected");
				sessionDisconnect(session);
			}
		}	
	}

	public void registerConvertor(Class<?> cl, Convertor conv) {
		// TODO put this elsewhere
		JettyJSONContextServer jsonContext = (JettyJSONContextServer) bayeux.getOption("jsonContext");
		jsonContext.getJSON().addConvertor(cl, conv);
	}
	
	public int getNumCompleted() {		
		return completedHITs.get();
	}

	void sessionView(ServerSession session, String hitId) {
		if( hitId != null ) {					
			
			// This session currently hasn't accepted this HIT 
			HITWorkerImpl prev = clientToHITWorker.replace(session, null);
						
			logger.info("New user is viewing HIT {}, previous user was {}", hitId, prev);
			
			session.setAttribute("hitId", hitId);
			
			// Try to add hitIds that we have no record of (clean reuse)
			if( !tracker.hitExistsInDB(hitId) )
				tracker.saveHITId(hitId);
			
		}
		else {
			System.out.println("Client " + session.getId() + " sent null hitId");
		}
	}

	HITWorkerImpl sessionAccept(ServerSession session, 
			String hitId, String assignmentId, String workerId) {				
		if( hitId == null ) {
			logger.warn("Received null hitId for session {}", session.getId());
			return null;
		}
		
		try {
			workerAuth.checkHITValid(hitId, workerId);				
		}
		catch (SessionCompletedException e) {
			SessionUtils.sendStatus(session, "completed", Messages.SESSION_COMPLETED);
			logger.info("Client connected to experiment after completion: "	+ hitId.toString());
			return null;
		} catch (SessionOverlapException e) {
			SessionUtils.sendStatus(session, Codec.status_sessionoverlap, Messages.SESSION_OVERLAP);
			return null;
		}
		
		try {
			workerAuth.checkWorkerLimits(hitId, assignmentId, workerId);
		} catch (SimultaneousSessionsException e) {			
			SessionUtils.sendStatus(session, Codec.status_simultaneoussessions, Messages.SIMULTANEOUS_SESSIONS);
			return null;
		} catch (TooManySessionsException e) {			
			SessionUtils.sendStatus(session, Codec.status_toomanysessions, Messages.TOO_MANY_SESSIONS);
			return null;
		}		
		
		// Save some extra information that we can access later, for comparison 					
		session.setAttribute("hitId", hitId);
		session.setAttribute("assignmentId", assignmentId);
		session.setAttribute("workerId", workerId);
		
		try {
			if( workerAuth.workerRequiresQuiz(workerId) ) {				
				QuizMaterials qm = workerAuth.getQuiz();
								
				// TODO: null quiz is passed for static client-side 											
				Map<String, Object> data = qm == null ?
						ImmutableMap.of("status", (Object) Codec.status_quizneeded) :
							ImmutableMap.of("status", Codec.status_quizneeded,	"quiz", qm.toData() );
				SessionUtils.sendServiceMsg(session, data);									
								
				return null;
			}
		} catch (TooManyFailsException e) {
			SessionUtils.sendStatus(session, Codec.status_failsauce, Messages.TOO_MANY_FAILS); 					
			return null;
		}
		
		HITWorkerImpl hitw = null;
		
		try {
			Session record = workerAuth.checkAssignment(hitId, assignmentId, workerId);

			// Find this HITWorker in table
			if( (hitw = hitWorkerTable.get(hitId, workerId)) == null ) {
				// create instance of HITWorker			
				hitw = new HITWorkerImpl(session, record);
				hitWorkerTable.put(hitId, workerId, hitw);
			}
			
			// Match session to HITWorker and vice versa
			ServerSession oldSession = hitw.cometdSession.get();
			if( oldSession == null || !session.equals(oldSession) ) {
				hitw.setServerSession(session);				
			}			
			clientToHITWorker.put(session, hitw);

			// Successful registration, save info
			tracker.saveIP(hitw, bayeux.getContext().getRemoteAddress().getAddress(), new Date());			

		} catch (ExpServerException e) {		

			if (e instanceof SessionExpiredException ) {					
				SessionUtils.sendStatus(session, null, Messages.EXPIRED_SESSION);

				logger.warn("Unexpected connection on expired session: " + hitId.toString());
			}
			else if (e instanceof SessionOverlapException ) {
				SessionUtils.sendStatus(session, Codec.status_sessionoverlap, Messages.SESSION_OVERLAP);
			}
			else {
				SessionUtils.sendStatus(session, null, "Unknown Error: " + e.toString());
			}				
		}
		
		// subclasses continue with additional logic if necessary 
		
		return hitw;
	}

	void sessionSubmit(ServerSession session) {
		
		// TODO write any logs for session		
		
		int completed = completedHITs.incrementAndGet();
		System.out.println(completed + " HITs completed");
		
		// TODO check the total number of possible different tasks as well - getTotalPuzzles()
		
		String workerId = (String) session.getAttribute("workerId");
		
		if( workerId != null ) {
			int additional = workerAuth.getSetLimit() - tracker.getSetSessionInfoForWorker(workerId).size();		
			SessionUtils.sendStatus(session, "completed", 
					"Thanks for your work! You may do " + additional + " more HITs in this session.");
		}
		else {
			logger.warn("No workerId metadata recorded for completed session {}", session.getId());
		}
	}

	void sessionReconnect(ServerSession session, HITWorkerImpl hitw) {
		// experiment should send state to user with this callback
		experiments.workerReconnected(hitw);
		hitw.reconnected();
	}
	
	void sessionDisconnect(ServerSession session) {
		HITWorkerImpl worker = clientToHITWorker.get(session);				
		if( worker != null ) {
			experiments.workerDisconnected(worker);
			worker.disconnected();
		}
		
		String workerHitId = worker == null ? null : worker.getHitId(); 				
		if( workerHitId != null ) {
			/* 
			 * TODO user can accept a hit then close window, but this is the same thing as 
			 * accepting multiple hits, holding and refreshing
			 * Fix with a notification receptor later.  
			 */
			if( !(experiments.workerIsInProgress(worker) || 
					SessionRecord.status(worker.record) == SessionStatus.COMPLETED) ) {
				/* If disconnected from lobby or earlier, clear session from worker Id list
				 * also clear the username that was stored from worker
				 * 
				 * BUT if in experiment, they need to wait	
				 */
				tracker.clearWorkerForSession(workerHitId);
			}
		}
		
		String sessionHitId = (String) session.getAttribute("hitId");		
		if( workerHitId != null && !workerHitId.equals(sessionHitId) || 
				sessionHitId != null && !sessionHitId.equals(workerHitId) ) {
			logger.error("Session and worker HIT IDs don't match for {}", session.getId());
		}
		
	}
	
	void rcvQuizResults(ServerSession session, QuizResults qr) {
		String workerId = (String) session.getAttribute("workerId");
		String hitId = (String) session.getAttribute("hitId");
		String assignmentId = (String) session.getAttribute("assignmentId");
		
		if( workerId == null ) {
			logger.error("Can't save quiz: unknown worker for {}", session.getId());
		}
		
		tracker.saveQuizResults(hitId, workerId, qr);

		// check if quiz failed
		if( workerAuth.quizPasses(qr) ) {
			logger.info("{} passed quiz", workerId);
			// Run the accept HIT checks again		
			sessionAccept(session, hitId, assignmentId, workerId);
		}
		else {
			if( workerAuth.tooManyFails(workerId) ) {
				logger.info("{} failed quiz too many times", workerId);
				SessionUtils.sendStatus(session, Codec.status_failsauce);
			}
			else {
				logger.info("{} failed quiz", workerId);
				SessionUtils.sendStatus(session, Codec.status_quizfail);
			}
		}		
	}

	void rcvInactiveTime(ServerSession session, long inactiveTime) {
		HITWorkerImpl worker = clientToHITWorker.get(session);
		
		if( worker == null ) {
			logger.error("Can't save inactivity: unknown worker for {}", session.getId());
		}
		
		worker.addInactiveTime(inactiveTime);
	}

	void rcvExperimentServiceMsg(ServerSession session,
			Map<String, Object> dataAsMap) {
		HITWorkerImpl worker = clientToHITWorker.get(session);
		
		if( worker != null ) experiments.rcvServiceMsg(worker, dataAsMap);
		else logger.warn("Message from unrecognized client: {}", session.getId());
	}

	boolean rcvExperimentBroadcastMsg(ServerSession session,
			Map<String, Object> dataAsMap) {
		HITWorkerImpl worker = clientToHITWorker.get(session);
		
		if( worker != null ) return experiments.rcvBroadcastMsg(worker, dataAsMap);
		else logger.warn("Message from unrecognized client: {}", session.getId());
		
		return false;
	}

	/**
	 * A group of workers has completed their tasks.
	 * @param group
	 * @return whether the server should shut down
	 */
	boolean groupCompleted(HITWorkerGroup group) {
		if( completedHITs.addAndGet(group.groupSize()) >= hitGoal ) {
			logger.info("Goal of " + hitGoal + " users reached!");
			
			// Quit the thread
			this.interrupt();						
			return true;
		}
		
		return false;
	}

	@Override
	public final void run() {
		Thread.currentThread().setName(this.getClass().getSimpleName());				
		
		Server server = null;
		try {
			server = jettyCometD.start(this);
		} catch (Exception e1) {			
			e1.printStackTrace();
		}
		
		bayeux = jettyCometD.getBayeux();		
		experiments.setReferences(bayeux);
		
		Thread hcThread = null;
		if( hitCont != null ) {
			(hcThread = new Thread(hitCont)).start();
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				// TODO add stuff from above to in here, like auto expiring hits
				
				logger.info("Shutdown initiated");								
			}
		});
		
		runServerInit();
		
	    // Hang out until goal # of HITs are reached and shutdown jetty server
		while( running && completedHITs.get() < hitGoal ) {			
			try { Thread.sleep(5000); }
			catch (InterruptedException e) {}
		}
				
		System.out.println("Deleting remaining HITs");
		
		if( hitCont != null ) {
			hitCont.disableAndShutdown();
			try {
				hcThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}			
		
		/* TODO send a message to people that took HITs after the deadline
		 * or still-connected clients		
		 */
		
		try {	
			// Sleep for a bit before shutting down jetty server
			if( debugMode ) Thread.sleep(1000); 
			else Thread.sleep(5 * 60 * 1000);
		}
		catch (Exception e ) {
			e.printStackTrace();
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
		this.interrupt();
	}

	protected abstract void runServerInit();

}