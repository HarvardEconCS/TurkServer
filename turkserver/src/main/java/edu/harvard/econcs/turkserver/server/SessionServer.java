package edu.harvard.econcs.turkserver.server;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.andrewmao.misc.Utils;

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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.Codec.LoginStatus;
import edu.harvard.econcs.turkserver.ExpServerException;
import edu.harvard.econcs.turkserver.Messages;
import edu.harvard.econcs.turkserver.SessionCompletedException;
import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.SessionOverlapException;
import edu.harvard.econcs.turkserver.SessionUnknownException;
import edu.harvard.econcs.turkserver.SimultaneousSessionsException;
import edu.harvard.econcs.turkserver.TooManySessionsException;
import edu.harvard.econcs.turkserver.mturk.TurkHITManager;
import edu.harvard.econcs.turkserver.server.mysql.DataTracker;

public abstract class SessionServer implements Runnable {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());	
	
	public static final String ATTRIBUTE = "turkserver.session";
	
	private final DataTracker<String> tracker;
	protected final TurkHITManager<String> turkHITs;
	protected final int hitGoal;
	
	protected final Server server;	
	protected final ContextHandlerCollection contexts;
	protected final ServletContextHandler context;
	protected final CometdServlet cometdServlet;
	
	protected BayeuxServerImpl bayeux;	
	
	protected BiMap<String, String> clientToId;	
	
	protected final AtomicInteger completedHITs;

	ConcurrentHashMap<String, Long> logStartTimes;
	ConcurrentHashMap<String, StringBuffer> sessionLogs;
	
	public SessionServer(DataTracker<String> tracker, TurkHITManager<String> thm,
			Resource[] resources, int hitGoal, int httpPort) {

		this.tracker = tracker;								

		this.turkHITs = thm;
		
		this.hitGoal = hitGoal;
		
		BiMap<String, String> bm = HashBiMap.create();
		this.clientToId = Maps.synchronizedBiMap(bm);		
		
		this.completedHITs = new AtomicInteger();
		
		this.logStartTimes = new ConcurrentHashMap<String, Long>();
		this.sessionLogs = new ConcurrentHashMap<String, StringBuffer>();
		
		// Set up the jetty server
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
			String oldId = clientToId.forcePut(session.getId(), null);
			
			if( oldId != null ) {
				logger.info(session.getId() + " reconnected, used to have id " + oldId.toString());
			}
		}

		@Override
		public void sessionRemoved(ServerSession session, boolean timedout) {
			String clientId = session.getId();
			String hitId = clientToId.get(clientId);
			
			if( timedout ) {
				Log.getRootLogger().info("Session " + clientId + " timed out");
				sessionDisconnect(clientId, hitId);
			}
			else {
				Log.getRootLogger().info("Session " + clientId + " disconnected");
				sessionDisconnect(clientId, hitId);
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

	protected void logReset(String logId) {						
		// Close any previous log if it was open
		StringBuffer log = sessionLogs.get(logId);
		if( log != null ) 
			log.delete(0, log.length());
		else {
			log = new StringBuffer();
			sessionLogs.put(logId, log);
		}
		
		logStartTimes.put(logId, System.currentTimeMillis());
		logString(logId, "started");		
	}

	protected synchronized void logString(String logId, String msg) {
		if( logId == null ) {
			System.out.println("Got null logId");
			return;
		}
		
		StringBuffer log = sessionLogs.get(logId);
		if( log != null ) {
			log.append(	String.format(
					"%s %s\n",
					Utils.clockStringMillis(System.currentTimeMillis() 
							- logStartTimes.get(logId)), 
					msg ) );
		}				
		else System.out.println(logId + "Log discarded: " + msg);
	}

	protected String logFlush(String logId) {				
		logString(logId, "finished");		

		StringBuffer log = sessionLogs.get(logId);
		if( log != null ) {
			return log.toString();				
		}					

		return null;
	}

	public void sessionView(String clientId, String hitId) {
		if( hitId != null ) {						
			clientToId.forcePut(clientId, hitId);
			
			try {
				// Try to add hitIds that we have no record of (clean reuse)
				if( !tracker.sessionExistsInDB(hitId) )
					tracker.saveHITId(hitId);
			} catch (SessionExpiredException e) {				
				logger.info("Expired session is being viewed...");
			}
		}
		else {
			System.out.println("Client " + clientId + " sent null hitId");
		}
	}

	protected LoginStatus sessionAccept(String clientId, String hitId, String assignmentId,
			String workerId) {
		LoginStatus ls = LoginStatus.ERROR;

		if( hitId != null ) {			
			try {				
				
				ls = tracker.registerAssignment(hitId, assignmentId, workerId);

				// Successful registration, save info
				tracker.saveIPForSession(hitId, 
						bayeux.getContext().getRemoteAddress().getAddress(), new Date());

				// Reset the log, no saved state
				clientToId.forcePut(clientId, hitId);
				
				// Save some extra information, that we can access later
				ServerSession session = bayeux.getSession(clientId);				
				session.setAttribute("hitId", hitId);
				session.setAttribute("assignmentId", assignmentId);
				session.setAttribute("workerId", workerId);

			} catch (ExpServerException e) {		

				if( e instanceof SessionUnknownException ) {
					sendException(clientId, null, Messages.UNRECOGNIZED_SESSION);
				}
				else if (e instanceof SimultaneousSessionsException) {
					sendException(clientId, null, 
							"There is another HIT registered to you right now. Look for it in your dashboard. " +
							"If you returned that HIT, you will have to wait until someone else takes it to participate.");
				}
				else if (e instanceof SessionExpiredException ) {					
					sendException(clientId, null, Messages.EXPIRED_SESSION);
					
					logger.warn("Unexpected connection on expired session: " + hitId.toString());
				}
				else if (e instanceof TooManySessionsException) {
					sendException(clientId, null, "You've already done enough for today. Please return this HIT and come back tomorrow.");
				}
				else if (e instanceof SessionOverlapException ) {
					sendException(clientId, null, Messages.SESSION_OVERLAP);
				}
				else if (e instanceof SessionCompletedException) {
					sendException(clientId, "completed", Messages.SESSION_COMPLETED);
					
					logger.info("Client connected to experiment after completion: "	+ hitId.toString());
				}
				else {
					sendException(clientId, null, "Unknown Error: " + e.toString());
				}				
			}
		}

		return ls;
	}

	public void sessionSubmit(String clientId, String hitId, String workerId) {				
		logFlush(hitId.toString());
		
		int completed = completedHITs.incrementAndGet();
		System.out.println(completed + " HITs completed");
		
		// TODO check the total number of possible different tasks as well - getTotalPuzzles()
		int additional = tracker.getSetLimit() - tracker.getSetSessionInfoForWorker(workerId).size();
		
		sendException(clientId, "completed", "Thanks for your work! You may do " + additional + " more HITs in this session.");				
	}

	public void sessionDisconnect(String clientId, String hitId) {
		if( hitId != null ) {
			tracker.sessionDisconnected(hitId);
		}				
	}

	protected void sendException(String clientId, String status, String msg) {
		Map<String, Object> errorMap = new HashMap<String, Object>();
		
		if( status != null ) errorMap.put("status", status);
		else errorMap.put("status", Codec.connectErrorAck);
		
		errorMap.put("msg", msg);
		
		ServerSession client = bayeux.getSession(clientId);
		client.deliver(client, "/service/user", errorMap, null);
	}

	protected String getSessionForClient(String clientId) {
		return clientToId.get(clientId);
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

	protected void runServerInit() {
		
	}

}