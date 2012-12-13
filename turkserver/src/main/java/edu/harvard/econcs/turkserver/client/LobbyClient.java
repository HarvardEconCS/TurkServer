package edu.harvard.econcs.turkserver.client;

import edu.harvard.econcs.turkserver.*;
import edu.harvard.econcs.turkserver.api.ClientLobbyController;

import java.util.Map;
import java.util.TreeMap;

import org.cometd.annotation.*;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;

public class LobbyClient extends SessionClient implements ClientLobbyController {

	public static enum State { DISCONNECTED, LOBBY, EXPERIMENT }

	volatile State state;
	boolean isReady = false;	
	
	private LobbyClient() {		
		super();
		
		isReady = false;
		state = State.DISCONNECTED;
	}
	
	/**
	 * Get a GroupClient that reflects a client class
	 * @param clientClass
	 * @return
	 * @throws Exception
	 */
	public static LobbyClient getWrappedClient(Class<?> clientClass) throws Exception {
		LobbyClient client = new LobbyClient();
		client.clientWrapper = new ClientAnnotationManager(client, clientClass);
		return client;
	}
	
	@Override
	public void connect(String url, String hitId, String assignmentId, String workerId) {
		super.connect(url, hitId, assignmentId, workerId);
		
		processor.process(new NetExpService());
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
				System.out.println(m.get("msg"));
				
				if( Codec.quizNeeded.equals(status.toString()) ) {
					@SuppressWarnings("unchecked")
					Map<String, Object> quizData = (Map<String, Object>) m.get("quiz");
					
					// TODO convert quizdata to quizzable format
					QuizMaterials qm = (QuizMaterials) quizData;
					
					clientWrapper.triggerQuiz(qm);		
				}
				else if( Codec.usernameNeeded.equals(status.toString()) ) {
					// We need a username
					clientWrapper.triggerRequestUsername();												
				}
				else if( Codec.connectLobbyAck.equals(status.toString()) ) {
					// We should be in the lobby
					state = State.LOBBY;
					logger.info("Client connected to lobby");
					
					clientWrapper.triggerJoinLobby();																																									
					updateLobbyStatus(null);
				}
				else if( Codec.connectExpAck.equals(status.toString())) {					
					logger.info("Client is (re)connecting to an experiment");
					
					// Subscribe to this channel
					String chan = m.get("channel").toString();					
					subscribeExpChannel(chan);
					
					if( state == State.LOBBY ) {
						state = State.EXPERIMENT;									
					}										;
				}
				else if( Codec.expFinishedAck.equals(status.toString() ) ) {
					logger.info("Connected to experiment that is already done");
					clientWrapper.triggerClientError(Codec.expFinishedAck);														
					LobbyClient.this.disconnect();
				}
				else if( Codec.startExpError.equals(status.toString()) ) {
					clientWrapper.triggerClientError(Codec.startExpError);					
					isReady = false;
					
					// TODO update lobby state as necessary
				}
				else if( Codec.doneExpMsg.equals(status.toString())) {
					clientWrapper.triggerFinishExperiment();			
					// Do nothing
				} 
				else if( Codec.batchFinishedMsg.equals(status.toString())) {
					clientWrapper.triggerClientError(Codec.batchFinishedMsg);					
					disconnect();					
				}
			}
			else {
				System.out.println("Service message unexpected:");
				System.out.println(m);
			}		
		}
		
		@Subscription("/lobby")
		public void lobbyUpdate(Message service) {
			// TODO process a lobby update (need usernames!)			
			
			if( state == State.EXPERIMENT ) {
				/* TODO Currently ignoring
				 * fix this in the future, but commented for now to eliminate a bug source
				 */
				logger.warning("Got experiment update while in lobby");
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


