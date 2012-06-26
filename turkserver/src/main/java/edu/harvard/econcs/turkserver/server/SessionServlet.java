package edu.harvard.econcs.turkserver.server;

import java.io.IOException;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.*;
import org.cometd.java.annotation.*;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.authorizer.GrantAuthorizer;
import org.cometd.server.ext.AcknowledgedMessagesExtension;
import org.cometd.server.ext.TimesyncExtension;
import org.eclipse.jetty.util.log.Log;

public abstract class SessionServlet<S extends SessionServer> extends GenericServlet {

	private static final long serialVersionUID = -3882966106597782108L;	
	
	protected S theServer;
	
	protected BayeuxServerImpl bayeux;
	protected ServerAnnotationProcessor processor;
	
	@SuppressWarnings("unchecked")
	@Override
	public void init() throws ServletException {
		super.init();
		
		theServer = (S) getServletContext().getAttribute(SessionServer.ATTRIBUTE);
		
		bayeux = (BayeuxServerImpl) 
				getServletContext().getAttribute(BayeuxServer.ATTRIBUTE);		
		
		// Create extensions
		bayeux.addExtension(new TimesyncExtension());
		bayeux.addExtension(new AcknowledgedMessagesExtension());
		
		// Wildcard authorizer - Deny unless granted
        bayeux.createIfAbsent("/**",new ServerChannel.Initializer()
        {
            public void configureChannel(ConfigurableServerChannel channel)
            {
                channel.addAuthorizer(GrantAuthorizer.GRANT_NONE);
            }
        });

        // Allow anybody to handshake
        bayeux.getChannel(ServerChannel.META_HANDSHAKE).addAuthorizer(GrantAuthorizer.GRANT_PUBLISH);             
                
        processor = new ServerAnnotationProcessor(bayeux) {
        	
        };
        
        processor.process(new Monitor());
        processor.process(new UserData());
        
        bayeux.addListener(theServer.new UserSessionListener());
        
	}
	
	public ServerAnnotationProcessor getProcessor() {
		return processor;
	}

	@Service("userdata")
    public class UserData {
		
	    @Configure ("/service/user")
	    protected void configureUser(ConfigurableServerChannel channel)
	    {
	        channel.addAuthorizer(GrantAuthorizer.GRANT_PUBLISH);
	        channel.setPersistent(true);
	    }
	    
		@Listener("/service/user")
		public void userStatus(ServerSession session, ServerMessage message) {
			Map<String, Object> data = message.getDataAsMap();
			String status = data.get("status").toString();
						
			String clientId = session.getId();
			String hitId = null;			
			try { hitId = data.get("hitId").toString(); } catch (NullPointerException e) {}
			
			if( "view".equals(status) ) {				
				Log.getRootLogger().info("HIT " + hitId + " is being viewed by " + clientId);
				theServer.sessionView(session.getId(), hitId);
			}
			else if( "accept".equals(status)) {
				String assignmentId = null, workerId = null;
				
				try { assignmentId = data.get("assignmentId").toString(); }	
				catch (NullPointerException e) { Log.getRootLogger().warn("Null assignmentId for " + clientId); }
				
				try { workerId = data.get("workerId").toString(); }	
				catch (NullPointerException e) { Log.getRootLogger().warn("Null workerId for " + clientId);}
				
				Log.getRootLogger().info("HIT " + hitId + " assignment " + assignmentId + " accepted by " + workerId);
												
				theServer.sessionAccept(clientId, hitId, assignmentId, workerId);					
				
			}
			else if( "submit".equals(status) ) {
				String workerId = null;
				try { workerId = data.get("workerId").toString(); }	
				catch (NullPointerException e) { Log.getRootLogger().warn("Null workerId for " + clientId);}
				
				Log.getRootLogger().info("HIT " + hitId + " submitting");
				theServer.sessionSubmit(clientId, hitId, workerId);
			}						
			
		}
				
	}

	@Service("monitor")
    public static class Monitor
    {
        @Listener("/meta/subscribe")
        public void monitorSubscribe(ServerSession session, ServerMessage message)
        {
            Log.getRootLogger().info("Monitored Subscribe from "+session+" for "+message.get(Message.SUBSCRIPTION_FIELD));
        }

        @Listener("/meta/unsubscribe")
        public void monitorUnsubscribe(ServerSession session, ServerMessage message)
        {
            Log.getRootLogger().info("Monitored Unsubscribe from "+session+" for "+message.get(Message.SUBSCRIPTION_FIELD));
        }

        @Listener("/meta/*")
        public void monitorMeta(ServerSession session, ServerMessage message)
        {
//            if (Log.isDebugEnabled())
                Log.getRootLogger().debug(message.toString());
        }
        
        @Listener("/service/*")
        public void monitorSvc(ServerSession session, ServerMessage message)
        {
//            if (Log.isDebugEnabled())
                Log.getRootLogger().info(message.toString());
        }
    }
	
	@Override
	public void service(ServletRequest req, ServletResponse res)
			throws ServletException, IOException {
		((HttpServletResponse) res).sendError(503);
	}
}
