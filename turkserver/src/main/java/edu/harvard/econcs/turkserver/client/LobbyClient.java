package edu.harvard.econcs.turkserver.client;

import edu.harvard.econcs.turkserver.*;
import edu.harvard.econcs.turkserver.api.ClientLobbyController;

import java.util.Map;
import java.util.TreeMap;

import org.cometd.annotation.*;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;

public class LobbyClient<C> extends SessionClient<C> implements ClientLobbyController {

	public static enum State { DISCONNECTED, LOBBY, EXPERIMENT }

	volatile State state;
	boolean isReady = false;	
	
	LobbyService lobbySvc = null;
	
	public LobbyClient() {		
		super();
		
		isReady = false;
		state = State.DISCONNECTED;
	}
	
	@Override
	public void connect(String url, String hitId, String assignmentId, String workerId) {
		super.connect(url, hitId, assignmentId, workerId);
		
		processor.process(new NetExpService());
		processor.process(lobbySvc = new LobbyService());
	}

	@Override
	public void disconnect() {
		if( bayeuxClient != null && bayeuxClient.isConnected() ) {
			bayeuxClient.getChannel("/lobby").unsubscribe();			
		}
			
		super.disconnect();
	}

	@Service
	public class NetExpService {
	
		@Session
		private ClientSession session;
		
		@Subscription("/service/user")
		public void serviceUser(Message service) {			
			Map<String, Object> m = service.getDataAsMap();
			Object status = m.get("status");
			
			if( status != null ) {				
				
				if( Codec.status_quizneeded.equals(status.toString()) ) {
					@SuppressWarnings("unchecked")
					Map<String, Object> quizData = (Map<String, Object>) m.get("quiz");
					
					// TODO convert quizdata to quizzable format
					QuizMaterials qm = (QuizMaterials) quizData;
					
					clientWrapper.triggerQuiz(qm);		
				}
				else if( Codec.status_usernameneeded.equals(status.toString()) ) {
					// We need a username
					clientWrapper.triggerRequestUsername();												
				}
				else if( Codec.status_connectlobby.equals(status.toString()) ) {
					// We should be in the lobby
					state = State.LOBBY;
					logger.info("Client connected to lobby");
					
					clientWrapper.triggerJoinLobby();																																							
					updateLobbyStatus(null);
				}
				else if( Codec.status_connectexp.equals(status.toString())) {					
					logger.info("Client is (re)connecting to an experiment");
										
					if( state == State.LOBBY ) {
						state = State.EXPERIMENT;
						// Unsubscribe from lobby updates
						if( lobbySvc != null ) processor.deprocess(lobbySvc);
					}										
				}				
				else if( Codec.startExpError.equals(status.toString()) ) {
					clientWrapper.triggerClientError(Codec.startExpError);					
					isReady = false;
					
					// TODO update lobby state as necessary
				}				
			}
			else {
				System.out.println("Service message unexpected:");
				System.out.println(m);
			}		
		}
		
	}
	
	@Service
	class LobbyService {
		
		@Subscription("/lobby")
		public void lobbyUpdate(Message service) {
			// TODO process a lobby update (need usernames!)			
			
			if( state == State.EXPERIMENT ) {
				/* TODO Currently ignoring
				 * fix this in the future, but commented for now to eliminate a bug source
				 */
				logger.warning("Got lobby update while in experiment");
				return;
				
//				gc.setStatus(StatusBar.returningToLobbyMsg);
//				gc.lobbyRedraw();
//				state = State.LOBBY;
			}
			
			// TODO process lobby information with correct data
			Map<String, Object> data = service.getDataAsMap();			
			clientWrapper.triggerUpdateLobby(data);			
		}
	}

	@Override
	public void sendUsername(String username) {
		Map<String, Object> userData = new TreeMap<String, Object>();
		userData.put("status", "username");
		userData.put("username", username);
		
		bayeuxClient.getChannel("/service/user").publish(userData);		
	}

	@Override
	public void updateLobbyReadiness(boolean isReady) {
		if( this.isReady != isReady ) {
			this.isReady = isReady;
			
			// Only update if changed			
			updateLobbyStatus(null);
		}
	}		
	
	@Override
	public void updateLobbyStatus(String statusMsg) {
		Map<String, Object> data = new TreeMap<String, Object>();
		
		data.put("ready", isReady);
		if( statusMsg != null ) data.put("msg", statusMsg);
		
		bayeuxClient.getChannel("/lobby").publish(data);				
	}

	@Override
	public void recordInactivity(long timeInactive) {
		Map<String, Object> data = new TreeMap<String, Object>();
		
		data.put("status", "inactive");
		data.put("time", timeInactive);
		
		bayeuxClient.getChannel("/service/user").publish(data);			
	}		

	@Override
	protected void handShakeFail() {
		super.handShakeFail();		
	}
	
	@Override
	protected void connectionClosed() {
		super.connectionClosed();		
		state = State.DISCONNECTED;
	}
	
}


