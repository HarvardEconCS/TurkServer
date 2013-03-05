package edu.harvard.econcs.turkserver.server;

import java.util.Map;

import javax.servlet.GenericServlet;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.cometd.annotation.AnnotationCometdServlet;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.BayeuxServer.BayeuxServerListener;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.CometdServlet;
import org.cometd.server.DefaultSecurityPolicy;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.harvard.econcs.turkserver.config.TSConfig;

@Singleton
public class JettyCometD {
	
	public static final String ATTRIBUTE = "edu.harvard.econcs.turkserver.jettycometd";

	private int initOrder = 1;
	
	protected final Server server;	
	protected final ContextHandlerCollection contexts;
	protected final ServletContextHandler context;
	protected final CometdServlet cometdServlet;
		
	@Inject
	JettyCometD(
			@Named(TSConfig.SERVER_RESOURCES) Resource[] resources,
			Configuration conf
			) {				
		int httpPort = conf.getInt(TSConfig.SERVER_HTTPPORT);
		
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
                
//        context.setAliases(true);              
        
        // Default servlet
        ServletHolder dftServlet = context.addServlet(DefaultServlet.class, "/");
        dftServlet.setInitOrder(initOrder++);
        
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
        comet.setInitParameter("jsonContext", org.cometd.server.JettyJSONContextServer.class.getCanonicalName());
        
        comet.setInitParameter("transports", org.cometd.websocket.server.WebSocketTransport.class.getCanonicalName());
//        comet.setInitParameter("transports","org.cometd.server.transport.LongPollingTransport");
        comet.setInitOrder(initOrder++);       
        
        context.setAttribute(ATTRIBUTE, this);
	}
	
	/**
	 * Add optional additional servlets before starting server
	 * @param servletPaths
	 */
	@Inject(optional=true)
	void addCustomServlets(
			@Named(TSConfig.SERVER_EXTRA_SERVLETS) 
			Map<Class<? extends GenericServlet>, String> servletPaths) {
		for( Map.Entry<Class<? extends GenericServlet>, String> e : servletPaths.entrySet() ) {
			addServlet(e.getKey(), e.getValue());
		}
	}
	
	/**
	 * Injects custom handlers for the server.
	 * @param handlerPaths
	 */
	@Inject(optional=true)
	void addCustomHandlers(
			@Named(TSConfig.SERVER_CUSTOM_HANDLERS)
			Map<Handler, String> handlerPaths
			) {
		for( Map.Entry<Handler, String> e : handlerPaths.entrySet() ) {
			addCustomHandler(e.getKey(), e.getValue());
		}
	}
	
	public ServletHolder addServlet(Class<? extends GenericServlet> servletClass, String pathSpec) {
		ServletHolder holder = context.addServlet(servletClass, pathSpec);  
        holder.setInitOrder(initOrder++);
        
        return holder;		
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

	BayeuxServer getBayeux() {
		return cometdServlet.getBayeux();
	}

	/**
	 * Start Jetty server and configure bayeux server
	 * @throws Exception 
	 */
	Server start(SessionServer sessions) throws Exception {
		context.setAttribute(SessionServer.ATTRIBUTE, sessions);
	    
		server.start();		
	    
	    BayeuxServerImpl bayeux = cometdServlet.getBayeux();	    
		bayeux.setSecurityPolicy(new DefaultSecurityPolicy());
	
		bayeux.addListener(new BayeuxServerListener() {
			
		});	
		
		return server;
	}
	
}
