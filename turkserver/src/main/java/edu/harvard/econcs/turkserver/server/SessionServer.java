package edu.harvard.econcs.turkserver.server;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.cometd.annotation.AnnotationCometdServlet;
import org.cometd.bayeux.server.BayeuxServer.BayeuxServerListener;
import org.cometd.bayeux.server.BayeuxServer.SessionListener;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.CometdServlet;
import org.cometd.server.DefaultSecurityPolicy;
import org.cometd.server.JettyJSONContextServer;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ajax.JSON.Convertor;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import edu.harvard.econcs.turkserver.mturk.TurkHITManager;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

@Singleton
public abstract class SessionServer implements Runnable {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	public static final String ATTRIBUTE = "turkserver.session";
			
	final ExperimentDataTracker tracker;
	final TurkHITManager turkHITs;
	final WorkerAuthenticator workerAuth;	
	final Experiments experiments;
	
	protected final int hitGoal;	
	
	protected final Server server;	
	protected final ContextHandlerCollection contexts;
	protected final ServletContextHandler context;
	protected final CometdServlet cometdServlet;
	
	protected BayeuxServerImpl bayeux;	
	
	protected ConcurrentMap<ServerSession, HITWorkerImpl> clientToHITWorker;
	protected Table<String, String, HITWorkerImpl> hitWorkerTable;
	
	protected final AtomicInteger completedHITs;	
	
	@Inject
	public SessionServer(			
			ExperimentDataTracker tracker,			
			TurkHITManager thm,
			WorkerAuthenticator workerAuth,
			Experiments experiments,
			Resource[] resources,
			Configuration config
			) throws ClassNotFoundException {		
		
		this.tracker = tracker;								
		this.turkHITs = thm;
		this.workerAuth = workerAuth;
		this.experiments = experiments;
		
		this.completedHITs = new AtomicInteger();
		
		/*
		 * Process configuration
		 */
		this.hitGoal = config.getInt(TSConfig.SERVER_HITGOAL);		
		int httpPort = config.getInt(TSConfig.SERVER_HTTPPORT);						
		
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
				
		/* 
		 * Set up the jetty server		 
		 */
		server = new Server();
		QueuedThreadPool qtp = new QueuedThreadPool();
		qtp.setMinThreads(5);
        qtp.setMaxThreads(200);
        server.setThreadPool(qtp);
                
		server.setGracefulShutdown(1000);
		server.setStopAtShutdown(true);
        
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(httpPort);
        connector.setMaxIdleTime(120000);
        connector.setLowResourcesMaxIdleTime(60000);
        connector.setLowResourcesConnections(20000);
        connector.setAcceptQueueSize(5000);
        server.addConnector(connector);
        
        SocketConnector bconnector = new SocketConnector();
        bconnector.setPort(httpPort+1);
        server.addConnector(bconnector);
		
        contexts = new ContextHandlerCollection();        
        server.setHandler(contexts);
        
        // Base files servlet
        context = new ServletContextHandler(contexts, "/" ,ServletContextHandler.SESSIONS);       
        context.setBaseResource(new ResourceCollection(resources));
        context.setAliases(true);              
        
        // Default servlet
        ServletHolder dftServlet = context.addServlet(DefaultServlet.class, "/");
        dftServlet.setInitOrder(1);
        
        // Cometd servlet
        cometdServlet = new AnnotationCometdServlet();
        
        ServletHolder comet = new ServletHolder(cometdServlet);
        context.addServlet(comet, "/cometd/*");
        
        comet.setInitParameter("timeout", "20000");
        comet.setInitParameter("interval", "100");
        comet.setInitParameter("maxInterval", "10000");
        comet.setInitParameter("multiFrameInterval", "5000");
        comet.setInitParameter("logLevel", "2");
        
        // for registering serialization and deserialization
        comet.setInitParameter("jsonContext", "org.cometd.server.JettyJSONContextServer");
        
        comet.setInitParameter("transports", "org.cometd.server.websocket.WebSocketTransport");
//        comet.setInitParameter("transports","org.cometd.server.transport.LongPollingTransport");
        comet.setInitOrder(2);       
        
        context.setAttribute(ATTRIBUTE, this);  
	}
	
	/**
	 * Add custom handlers to the server.
	 * For advanced users that have custom dynamic content.
	 * @param handler
	 */
	public void addCustomHandler(Handler handler, String contextPath) {
        // Additional custom handlers
		
		ContextHandler ch = new ContextHandler(contextPath);
		ch.setHandler(handler);
		
		// Add this to the beginning of the collection of handlers
		Handler[] handlers = contexts.getHandlers();
		contexts.setHandlers((Handler[]) ArrayUtils.addAll(new Handler[] {ch}, handlers));		
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
				clientToHITWorker.putIfAbsent(session, null);
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
			sendStatus(session, "completed", Messages.SESSION_COMPLETED);
			logger.info("Client connected to experiment after completion: "	+ hitId.toString());
			return null;
		} catch (SessionOverlapException e) {
			sendStatus(session, null, Messages.SESSION_OVERLAP);
			return null;
		}
		
		try {
			workerAuth.checkWorkerLimits(hitId, assignmentId, workerId);
		} catch (SimultaneousSessionsException e) {			
			sendStatus(session, null, 
					"There is another HIT registered to you right now. Look for it in your dashboard. " +
					"If you returned that HIT, please try participating later.");
			return null;
		} catch (TooManySessionsException e) {			
			sendStatus(session, null, 
					"You've already done enough for today. " +
					"Please return this HIT and come back tomorrow.");
			return null;
		}		
		
		try {
			if( workerAuth.workerRequiresQuiz(workerId) ) {				
				QuizMaterials qm = workerAuth.getQuiz();
				
				if( qm != null ) {
					Map<String, Object> data = ImmutableMap.of(
							"status", Codec.quizNeeded,
							"quiz", qm.toData()
							);			
					
					sendServiceMsg(session, data);									
				}				
				return null;
			}
		} catch (TooManyFailsException e) {			
			sendStatus(session, null, 
					"Sorry, you've failed the quiz too many times. " +
					"Please return this HIT and try again later.");
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

			// Save some extra information that we can access later, for comparison 					
			session.setAttribute("hitId", hitId);
			session.setAttribute("assignmentId", assignmentId);
			session.setAttribute("workerId", workerId);

		} catch (ExpServerException e) {		

			if (e instanceof SessionExpiredException ) {					
				sendStatus(session, null, Messages.EXPIRED_SESSION);

				logger.warn("Unexpected connection on expired session: " + hitId.toString());
			}
			else if (e instanceof SessionOverlapException ) {
				sendStatus(session, null, Messages.SESSION_OVERLAP);
			}

			else {
				sendStatus(session, null, "Unknown Error: " + e.toString());
			}				
		}
		
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
			sendStatus(session, "completed", "Thanks for your work! You may do " + additional + " more HITs in this session.");
		}
		else {
			logger.warn("No workerId metadata recorded for completed session {}", session.getId());
		}
	}

	void sessionDisconnect(ServerSession session) {
		HITWorkerImpl worker = clientToHITWorker.get(session);				
		if( worker != null ) {
			experiments.workerDisconnected(worker);
		}
		
		String workerHitId = worker == null ? null : worker.getHitId(); 				
		if( workerHitId != null ) {
			tracker.sessionDisconnected(workerHitId);
		}
		
		String sessionHitId = (String) session.getAttribute("hitId");		
		if( workerHitId != null && !workerHitId.equals(sessionHitId) || 
				sessionHitId != null && !sessionHitId.equals(workerHitId) ) {
			logger.error("Session and worker HIT IDs don't match for {}", session.getId());
		}
		
	}

	void sendStatus(ServerSession session, String status, String msg) {
		Map<String, Object> errorMap = new HashMap<String, Object>();
		
		if( status != null ) errorMap.put("status", status);
		else errorMap.put("status", Codec.connectErrorAck);
		
		errorMap.put("msg", msg);
						
		sendServiceMsg(session, errorMap);
	}
	
	void sendStatus(ServerSession session, String status) {
		Map<String, String> data = null;
		
		if( status != null ) 
			data = ImmutableMap.of("status", status);
		else
			data = ImmutableMap.of("status", Codec.connectErrorAck);				
						
		sendServiceMsg(session, data);
	}
	
	void sendServiceMsg(ServerSession session, Object data) {
		session.deliver(session, "/service/user", data, null);
	}
	
	void rcvQuizResults(ServerSession session, QuizResults qr) {
		String workerId = (String) session.getAttribute("workerId");
		String hitId = (String) session.getAttribute("hitId");
		
		if( workerId == null ) {
			logger.error("Can't save quiz: unknown worker for {}", session.getId());
		}
		
		tracker.saveQuizResults(hitId, workerId, qr);

		// check if quiz failed
		if( workerAuth.quizPasses(qr) ) {
			/* TODO quiz passed? allow lobby login
			 * careful - this assumes that quiz always precedes username
			 */
			sendStatus(session, "username");	
		}
		else {
			sendStatus(session, "quizfail");	
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

	@Override
	public final void run() {
		Thread.currentThread().setName(this.getClass().getSimpleName());				
		
	    try {
			server.start();
		} catch (Exception e) {			
			e.printStackTrace();
			return;
		}
	    
	    bayeux = cometdServlet.getBayeux();
		bayeux.setSecurityPolicy(new DefaultSecurityPolicy());

		bayeux.addListener(new BayeuxServerListener() {
			
		});
		
		if( turkHITs != null ) {
			new Thread(turkHITs).start();
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				// TODO add stuff from above to in here, like auto expiring hits
				
				logger.info("Shutdown initiated");								
			}
		});
		
		runServerInit();
		
	    // Hang out until goal # of HITs are reached and shutdown jetty server
		while( completedHITs.get() < hitGoal ) {			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
		}
		
		System.out.println("Deleting remaining HITs");
		
		if( turkHITs != null ) {
			turkHITs.expireRemainingHITs();
		}			
		
		// TODO send a message to people that took HITs after the deadline
		
		try {			
			// Sleep for a bit before shutting down jetty server
			Thread.sleep(5 * 60 * 1000);
		}
		catch (InterruptedException e ) {
			e.printStackTrace();
		}
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

	protected abstract void runServerInit();

}