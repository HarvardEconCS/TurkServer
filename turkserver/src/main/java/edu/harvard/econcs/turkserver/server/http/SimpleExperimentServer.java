/**
 * 
 */
package edu.harvard.econcs.turkserver.server.http;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.andrewmao.misc.Utils;

import org.cometd.bayeux.server.*;
import org.cometd.java.annotation.AnnotationCometdServlet;
import org.cometd.server.CometdServlet;
import org.cometd.server.DefaultSecurityPolicy;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import edu.harvard.econcs.turkserver.ExpServerException;
import edu.harvard.econcs.turkserver.Messages;
import edu.harvard.econcs.turkserver.SessionCompletedException;
import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.SessionOverlapException;
import edu.harvard.econcs.turkserver.SessionUnknownException;
import edu.harvard.econcs.turkserver.SimultaneousSessionsException;
import edu.harvard.econcs.turkserver.TooManySessionsException;
import edu.harvard.econcs.turkserver.mturk.TurkHITManager;
import edu.harvard.econcs.turkserver.server.mysql.SimpleDataTracker;

/**
 * @author mao
 * 
 * A simple experiment server supporting one-way communication via JSON
 * with javascript clients.
 * 
 * Useful for experiments that don't require interaction between clients
 *
 */
public abstract class SimpleExperimentServer implements Runnable {	
	
	public static final String ATTRIBUTE = "turkserver.simple-experiment";
	
	private final TurkHITManager<String> turkHITs;
	
	private final int hitGoal;	
	protected final SimpleDataTracker tracker;
	
	protected final Server server;
	private final CometdServlet cometdServlet; 
	
	protected BayeuxServer bayeux;
	
	private ConcurrentHashMap<String, String> clientToHITId;
	
	private ConcurrentHashMap<String, Long> startTimes;
	private ConcurrentHashMap<String, StringBuffer> sessionLogs;
	
	private final AtomicInteger completedHITs;
	
	public SimpleExperimentServer(
			SimpleDataTracker userTracker,
			TurkHITManager<String> thm,
			Class<? extends SimpleExperimentServlet<?>> servletClass,
			Resource[] resources,
			int hitGoal,
			int httpPort) throws Exception {
		
		this.tracker = userTracker;
		this.turkHITs = thm;
		this.hitGoal = hitGoal;
		
		this.clientToHITId = new ConcurrentHashMap<String, String>();
		
		this.startTimes = new ConcurrentHashMap<String, Long>();
		this.sessionLogs = new ConcurrentHashMap<String, StringBuffer>();
		
		this.completedHITs = new AtomicInteger();
		
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
		
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);
        
        // Base files servlet
        ServletContextHandler context = new ServletContextHandler(contexts,"/",ServletContextHandler.SESSIONS);        
        context.setBaseResource(new ResourceCollection(resources));
        
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
        comet.setInitParameter("logLevel", "1");
        
        comet.setInitParameter("transports", "org.cometd.server.websocket.WebSocketTransport");
//        comet.setInitParameter("transports","org.cometd.server.transport.LongPollingTransport");
        comet.setInitOrder(2);
        
        ServletHolder experiment = context.addServlet(servletClass, "/exp");  
        experiment.setInitOrder(3);
        
        context.setAttribute(ATTRIBUTE, this);
        
        // TODO init server extensions
	}
	
	public int getNumCompleted() {		
		return completedHITs.get();
	}

	protected void logReset(String hitId) {						
		// Close any previous log if it was open
		StringBuffer log = sessionLogs.get(hitId);
		if( log != null ) 
			log.delete(0, log.length());
		else {
			log = new StringBuffer();
			sessionLogs.put(hitId, log);
		}
		
		startTimes.put(hitId, System.currentTimeMillis());
		logString(hitId, "started");		
	}
	
	protected synchronized void logString(String hitId, String msg) {
		if( hitId == null ) {
			System.out.println("Got null hitId");
			return;
		}
		
		StringBuffer log = sessionLogs.get(hitId);
		if( log != null ) {
			log.append(	String.format(
					"%s %s\n",
					Utils.clockStringMillis(System.currentTimeMillis() - startTimes.get(hitId)), 
					msg ) );
		}				
		else System.out.println(hitId + "Log discarded: " + msg);
	}
	
	protected void logFlush(String hitId) {				
		logString(hitId, "finished");		
		
		StringBuffer log = sessionLogs.get(hitId);
		if( log != null ) {
			String data = log.toString();
			
			// Save to file
//			String path = NPuzzleSettings.config.getString(NPuzzleSettings.RESULT_DIR);
//			String filename = path + "/" + sessionId.toString(16) + ".log.gz";
//			FileObjUtils.writeToGZIP(filename, data);
			
			// Save to db
			tracker.saveSessionLog(hitId, data);
		}					
	}
	
	public void sessionView(String clientId, String hitId) {
		if( hitId != null ) {						
			clientToHITId.put(clientId, hitId);
		}
		else {
			System.out.println("Client " + clientId + " sent null hitId");
		}
	}
	
	protected boolean sessionAccept(String clientId, String hitId,
			String assignmentId, String workerId) {
		if( hitId != null ) {			
						
			try {				
				tracker.registerAssignment(hitId, assignmentId, workerId);
				
				tracker.saveIPForSession(hitId, 
						bayeux.getContext().getRemoteAddress().getAddress(), new Date());
				
				// Reset the log, no saved state
				clientToHITId.put(clientId, hitId);
				logReset(hitId);
				
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
				}
				else if (e instanceof TooManySessionsException) {
					sendException(clientId, null, "You've already done enough for today. Please return this HIT and come back tomorrow.");
				}
				else if (e instanceof SessionOverlapException ) {
					sendException(clientId, null, Messages.SESSION_OVERLAP);
				}
				else if (e instanceof SessionCompletedException) {
					sendException(clientId, "completed", Messages.SESSION_COMPLETED);									
				}
				else {
					sendException(clientId, null, "Unknown Error");
				}
																		
				return false;
			}
		}
		
		return true;
	}
	
	public void sessionSubmit(String clientId, String hitId, String workerId) {				
		logFlush(hitId);
		
		int completed = completedHITs.incrementAndGet();
		System.out.println(completed + " HITs completed");
		
		int additional = Math.min(tracker.getSetLimit(), getTotalPuzzles()) - tracker.getSetSessionInfoForWorker(workerId).size();
		
		sendException(clientId, "completed", "Thanks for your work! You may do " + additional + " more HITs in this session.");				
	}
	
	public void sessionDisconnect(String clientId, String hitId) {
		tracker.sessionDisconnected(hitId);
		
		// TODO remove client session
	}
	
	protected void sendException(String clientId, String status, String msg) {
		Map<String, Object> errorMap = new HashMap<String, Object>();
		
		if( status != null ) errorMap.put("status", status);
		else errorMap.put("status", "error");
		
		errorMap.put("msg", msg);
		
		ServerSession client = bayeux.getSession(clientId);
		client.deliver(client, "/service/user", errorMap, null);
	}
	
	protected String getSessionForClient(String clientId) {
		return clientToHITId.get(clientId);
	}

	protected abstract int getTotalPuzzles();

	@Override
	public void run() {
		Thread.currentThread().setName(this.getClass().getSimpleName());
		
        try {
			server.start();
		} catch (Exception e) {			
			e.printStackTrace();
			return;
		}
        
        bayeux = cometdServlet.getBayeux();
		bayeux.setSecurityPolicy(new DefaultSecurityPolicy());
		
		if( turkHITs != null ) {
			new Thread(turkHITs).start();
		}
		
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
	
	
}
