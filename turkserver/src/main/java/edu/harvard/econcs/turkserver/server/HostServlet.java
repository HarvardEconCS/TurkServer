package edu.harvard.econcs.turkserver.server;

import java.util.Map;

import javax.servlet.ServletException;

import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.annotation.Configure;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.server.authorizer.GrantAuthorizer;

import edu.harvard.econcs.turkserver.QuizResults;


public class HostServlet<T extends ExperimentServer<T>> extends SessionServlet<HostServer<T>> {

	private static final long serialVersionUID = 8755450296324273985L;
	
	@Override
	public void init() throws ServletException {		
		super.init();
		
		processor.process(new LobbyService());
		
		if (bayeux.getLogger().isDebugEnabled())
            System.err.println(bayeux.dump());
	}
	
	@Service
	public class LobbyService {
		
		@Configure("/lobby")
		public void configureLobby(ConfigurableServerChannel channel) {
			channel.addAuthorizer(GrantAuthorizer.GRANT_SUBSCRIBE_PUBLISH);
			channel.setPersistent(true);
		}
		
		@Listener("/lobby")
		public boolean listenLobby(ServerSession session, ServerMessage message) {
			
			Map<String,Object> data = message.getDataAsMap();
			String clientId = session.getId();
			
			if( data.containsKey("status") ) {
				if( "join".equals(data.get("status".toString()))) {
					return theServer.lobbyLogin(
							theServer.getSessionForClient(clientId), data.get("username").toString());
				}
				else {
					// broadcast all quit (or other) messages
					return true;
				}
			}
			else if( data.containsKey("ready") ) {
				boolean isReady = Boolean.parseBoolean(data.get("ready").toString());						
				
				return theServer.lobbyUpdate(theServer.getSessionForClient(clientId), isReady);
			}				
			
			return true;
		}
		
		@Listener("/service/user")
		public void listenUser(ServerSession session, ServerMessage message) {
			Map<String,Object> data = message.getDataAsMap();
			String clientId = session.getId();
			String status = data.get("status").toString();
			
			if( "quizresults".equals(status) ) {
				QuizResults qr = new QuizResults();
				qr.correct = Integer.parseInt(data.get("correct").toString());
				qr.total = Integer.parseInt(data.get("total").toString());
								
				theServer.sendQuizResults(theServer.getSessionForClient(clientId), qr);				
			}
			else if( "username".equals(status) ) {
				
				String username = data.get("username").toString();
				theServer.lobbyLogin(theServer.getSessionForClient(clientId), username);
				
			}
			else if( "inactive".equals(status) ) {
				long inactiveTime = Long.parseLong(data.get("time").toString());
				theServer.addInactiveTime(theServer.getSessionForClient(clientId), inactiveTime);
			}
		}
		
		@Configure("/service/experiment/*")
		public void configureServiceExperiment(ConfigurableServerChannel channel) {
			// TODO only allow pub/sub for clients that are part of this experiment
			channel.addAuthorizer(GrantAuthorizer.GRANT_SUBSCRIBE_PUBLISH);			
		}
		
		@Listener("/service/experiment/*")
		public void listenServiceExperiment(ServerSession session, ServerMessage message) {													
			// Deliver this to the appropriate experiment server
			theServer.experimentServiceMsg(session.getId(), message.getDataAsMap());			
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
				return theServer.experimentBroadcastMsg(session.getId(), message.getDataAsMap());
			}
			
		}

	}	


}
