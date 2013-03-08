package edu.harvard.econcs.turkserver.server;

import java.io.IOException;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.cometd.annotation.*;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.*;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.authorizer.GrantAuthorizer;
import org.cometd.server.ext.AcknowledgedMessagesExtension;
import org.cometd.server.ext.TimesyncExtension;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.schema.Quiz;

public class SessionServlet extends GenericServlet {

	private static final long serialVersionUID = -3882966106597782108L;	
	
	protected JettyCometD jettyServer;
	protected SessionServer sessions;
	
	protected BayeuxServerImpl bayeux;
	protected ServerAnnotationProcessor processor;
		
	@Override
	public void init() throws ServletException {
		super.init();
		
		sessions = (SessionServer) getServletContext().getAttribute(SessionServer.ATTRIBUTE);
		jettyServer = (JettyCometD) getServletContext().getAttribute(JettyCometD.ATTRIBUTE);		
		bayeux = (BayeuxServerImpl)	getServletContext().getAttribute(BayeuxServer.ATTRIBUTE);	
		
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
                
        processor = new ServerAnnotationProcessor(bayeux);
        
        // Debugging service
        processor.process(new Monitor());
        
        processor.process(new UserData());
        processor.process(new ExperimentData());
        
        // Watch for connect/disconnects
        bayeux.addListener(sessions.new UserSessionListener());        
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
			
			/* 
			 * TODO compare this hitId with the session metadata
			 * Remove requirements for client to send hitId every time
			 * 
			 * TODO make this logger class the same as the other ones
			 */
			
			String hitId = null;			
			try { hitId = data.get("hitId").toString(); } catch (NullPointerException e) {}
			
			if( Codec.hitView.equals(status) ) {				
				sessions.logger.info("HIT " + hitId + " is being viewed by " + clientId);
				
				sessions.sessionView(session, hitId);
			}
			else if( Codec.hitAccept.equals(status)) {
				String assignmentId = null, workerId = null;
				
				try { assignmentId = data.get("assignmentId").toString(); }	
				catch (NullPointerException e) { sessions.logger.warn("Null assignmentId for " + clientId); }
				
				try { workerId = data.get("workerId").toString(); }	
				catch (NullPointerException e) { sessions.logger.warn("Null workerId for " + clientId);}
				
				sessions.logger.info("HIT " + hitId + " assignment " + assignmentId + " accepted by " + workerId);
												
				sessions.sessionAccept(session, hitId, assignmentId, workerId);									
			}
			else if( Codec.quizResults.equals(status) ) {
				int correct = Integer.parseInt(data.get("correct").toString());
				int total = Integer.parseInt(data.get("total").toString());
				
				Quiz qr = new Quiz();				
				
				qr.setNumCorrect(correct);
				qr.setNumTotal(total);
				qr.setScore(1.0 * correct / total);
				qr.setAnswers(data.get("answers").toString());
								
				sessions.rcvQuizResults(session, qr);				
			}
			else if( "inactive".equals(status) ) {
				long inactiveTime = Long.parseLong(data.get("time").toString());
				sessions.rcvInactiveTime(session, inactiveTime);
			}
			else if( Codec.hitSubmit.equals(status) ) {								
				String survey = (String) data.get("comments");
				
				sessions.sessionSubmit(session, survey);
			}
			else {
				sessions.logger.warn("Unrecognized message " + data);
			}
		}				
	}
	
	@Service("experiment")
	public class ExperimentData {
		
		@Configure("/service/experiment/*")
		public void configureServiceExperiment(ConfigurableServerChannel channel) {
			// TODO only allow pub/sub for clients that are part of this experiment
			channel.addAuthorizer(GrantAuthorizer.GRANT_SUBSCRIBE_PUBLISH);			
		}
		
		@Listener("/service/experiment/*")
		public void listenServiceExperiment(ServerSession session, ServerMessage message) {													
			// Deliver this to the appropriate experiment server
			sessions.rcvExperimentServiceMsg(session, message.getDataAsMap());			
		}
		
		@Configure("/experiment/*")
		public void configureExperiment(ConfigurableServerChannel channel) {
			// TODO only allow pub/sub for clients that are part of this experiment
			// No need for persistent, leaves when last client is gone
			channel.addAuthorizer(GrantAuthorizer.GRANT_SUBSCRIBE_PUBLISH);			
		}
		
		@Listener("/experiment/*")
		public boolean listenExperiment(ServerSession session, ServerMessage message) {			
			if( session.isLocalSession() ) {
				// Always forward broadcast messages generated locally
				return true;				
			}
			else {
				// Deliver this to the appropriate experiment server			
				return sessions.rcvExperimentBroadcastMsg(session, message.getDataAsMap());
			}
			
		}
	}

	@Service("monitor")
    public class Monitor
    {
        @Listener("/meta/subscribe")
        public void monitorSubscribe(ServerSession session, ServerMessage message)
        {
        	sessions.logger.debug("Monitored Subscribe from "+session+" for "+message.get(Message.SUBSCRIPTION_FIELD));
        }

        @Listener("/meta/unsubscribe")
        public void monitorUnsubscribe(ServerSession session, ServerMessage message)
        {
        	sessions.logger.debug("Monitored Unsubscribe from "+session+" for "+message.get(Message.SUBSCRIPTION_FIELD));
        }

        @Listener("/meta/*")
        public void monitorMeta(ServerSession session, ServerMessage message)
        {
//            if (sessions.logger.isDebugEnabled())
        	sessions.logger.debug(message.toString());
        }
        
        @Listener("/service/*")
        public void monitorSvc(ServerSession session, ServerMessage message)
        {
//            if (sessions.logger.isDebugEnabled())
        	sessions.logger.debug(message.toString());
        }
    }
	
	@Override
	public void service(ServletRequest req, ServletResponse res)
			throws ServletException, IOException {
		((HttpServletResponse) res).sendError(503);
	}
}
