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

import edu.harvard.econcs.turkserver.Codec;

public class GroupServlet extends SessionServlet {

	private static final long serialVersionUID = 8755450296324273985L;
	
	protected GroupServer theServer;
	
	@Override
	public void init() throws ServletException {		
		super.init();
		
		theServer = (GroupServer) getServletContext().getAttribute(SessionServer.ATTRIBUTE);
		
		processor.process(new LobbyService());
		
		if (bayeux.getLogger().isDebugEnabled())
            System.err.println(bayeux.dump());		
	}
	
	@Service("lobby")
	public class LobbyService {
		
		@Configure("/lobby")
		public void configureLobby(ConfigurableServerChannel channel) {
			channel.addAuthorizer(GrantAuthorizer.GRANT_SUBSCRIBE_PUBLISH);
			channel.setPersistent(true);
		}
		
		@Listener("/lobby")
		public boolean listenLobby(ServerSession session, ServerMessage message) {
			
			Map<String,Object> data = message.getDataAsMap();			
			
			if( data.containsKey("status") ) {
				if( "join".equals(data.get("status".toString()))) {
					return theServer.lobbyLogin(
							session, data.get("username").toString());
				}
				else {
					// broadcast all quit (or other) messages
					return true;
				}
			}
			else if( data.containsKey("ready") ) {
				boolean isReady = Boolean.parseBoolean(data.get("ready").toString());						
				
				return theServer.lobbyUpdate(
						session, isReady);
			}				
			
			return true;
		}
		
		@Listener("/service/user")
		public void listenUser(ServerSession session, ServerMessage message) {
			Map<String,Object> data = message.getDataAsMap();
			
			String status = data.get("status").toString();
			
			 if( Codec.usernameReply.equals(status) ) {
				
				String username = data.get("username").toString();
				theServer.lobbyLogin(session, username);
				
			}
			
		}
		
	}	

}
