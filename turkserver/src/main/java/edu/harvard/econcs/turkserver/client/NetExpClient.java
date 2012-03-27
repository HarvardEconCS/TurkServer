package edu.harvard.econcs.turkserver.client;

import edu.harvard.econcs.turkserver.*;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JOptionPane;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.java.annotation.Service;
import org.cometd.java.annotation.Session;
import org.cometd.java.annotation.Subscription;

public abstract class NetExpClient extends SessionClient<BigInteger> implements AbstractExpClient {
	
	protected GUIController<? extends AbstractExpClient> gc;
	protected State state;

	public final BigInteger sessionID;
	public final String assignmentId;
	public final String workerId;	

	boolean isReady = false;	

	private String expBroadcastChan;
	private String expServiceChan;
	
	private MessageListener broadcastListener = null;
	private MessageListener serviceListener = null;
	
	public NetExpClient(BigInteger sessionID, String url, String assignmentId, String workerId) {
		
		super(url, sessionID, assignmentId, workerId);
		
		this.sessionID = sessionID;		
		this.assignmentId = assignmentId;
		this.workerId = workerId;

		state = State.DISCONNECTED;
		isReady = false;

		processor.process(new NetExpService());		
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
					QuizMaterials qm = null;
					
					doQuiz(qm);			
				}
				else if( Codec.usernameNeeded.equals(status.toString()) ) {
					// We need a username
					promptForUsername();													
				}
				else if( Codec.connectLobbyAck.equals(status.toString()) ) {
					// We should be in the lobby
					state = State.LOBBY;
					
					gc.setStatus("Connected to lobby");
					logger.info("Client connected to lobby");
																															
					updateLobbyStatus(null);
				}
				else if( Codec.connectExpAck.equals(status.toString())) {
					gc.setStatus("Connecting to game");
					logger.info("Client is (re)connecting to an experiment");
					
					// Subscribe to this channel
					String chan = m.get("channel").toString();					
					subscribeExpChannel(chan);
					
					if( state == State.LOBBY ) {
						state = State.EXPERIMENT;
						gc.setStatus(StatusBar.enteringExpMsg);
						gc.experimentRedraw();				
					}
					
					// request new data? if necessary
					refreshData();
				}
				else if( Codec.expFinishedAck.equals(status.toString() ) ) {
					// Connected to exp that is already finished. Enable the submit.
					// TODO fix this if want to allow view graph for finished exp
					gc.blankRedraw("Experiment already finished");
					gc.setStatus("Experiment already finished.");
					logger.info("Connected to experiment that is already done");
					gc.popMsg("This experiment is already finished.\n" +
							"Please submit the HIT below.");
					enableSubmit();
					NetExpClient.super.disconnect();
				}
				else if( Codec.startExpError.equals(status.toString()) ) {
					// Server unilaterally disabled due to error, disable join button
					isReady = false;
					gc.getLobby().setJoinEnabled(false);
					gc.popMsg("Unable to start experiment!" +
							"There might be a problem with the server." +
							"You may try again, or just return the HIT.",
							"Internal Server Error", JOptionPane.ERROR_MESSAGE);
					
					// TODO update lobby state as necessary
				}
				else if( Codec.doneExpMsg.equals(status.toString())) {
					gc.setStatus(StatusBar.finishedExpMsg);
					// Do nothing					
				} 
				else if( Codec.batchFinishedMsg.equals(status.toString())) {
					gc.blankRedraw("All games finished");
					gc.setStatus(StatusBar.batchFinishedMsg);
					gc.popMsg("All games for this batch have been completed.\n" +
							"If you have signed up for notifications, we will let you know\n" +
							"when we post more games. Please return the HIT.",
							"Game Batch Completed", JOptionPane.WARNING_MESSAGE);
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
			
			Map<String, Object> data = service.getDataAsMap();			
			// TODO process lobby information with correct data
			
			
			LobbyUpdateResp lup = (LobbyUpdateResp) service.getData();
			gc.getLobby().updateModel(lup);
			gc.setStatus(lup.joinEnabled 
					? StatusBar.lobbyReadyMsg 
							: StatusBar.lobbyWaitingMsg );
		}
	}
	
	public void subscribeExpChannel(String chan) {
		expBroadcastChan = Codec.expChanPrefix + chan;
		expServiceChan = Codec.expSvcPrefix + chan;
		
		broadcastListener = new MessageListener() {
			@Override
			public void onMessage(ClientSessionChannel channel, Message message) {				
				rcvBroadcastMsg(message.getClientId(), message.getDataAsMap());
			}			
		};
		
		serviceListener = new MessageListener() {
			@Override
			public void onMessage(ClientSessionChannel channel, Message message) {				
				rcvServiceMsg(message.getDataAsMap());
			}			
		};
		
		bayeuxClient.getChannel(expBroadcastChan).subscribe(broadcastListener);
		bayeuxClient.getChannel(expServiceChan).addListener(serviceListener);		
	}

	/**
	 * Requests a full update from the server/b
	 */
	protected abstract void refreshData();

	@Override
	public void disconnect() {
		bayeuxClient.getChannel("/lobby").unsubscribe();
		
		// unsubscribe from any experiment channel too
		if( broadcastListener != null ) {
			bayeuxClient.getChannel(expBroadcastChan).unsubscribe(broadcastListener);
			broadcastListener = null;
		}
		if( serviceListener != null ) {
			bayeuxClient.getChannel(expServiceChan).removeListener(serviceListener);
			serviceListener = null;
		}
			
		super.disconnect();
	}

	public void setGC(GUIController<? extends AbstractExpClient> gc) { this.gc = gc; }	
	
	public void doQuiz(QuizMaterials qm) {
		QuizResults qr = gc.doQuiz(qm);
		
		Map<String, Object> quizResults = new TreeMap<String, Object>();
		quizResults.put("status", "quizresults");
		quizResults.put("correct", qr.correct);
		quizResults.put("total", qr.total);
		
		bayeuxClient.getChannel("/service/user").publish(quizResults);		
	}
	
	public void promptForUsername() {
		gc.setStatus("Waiting for nickname...");

		String username = null;
		do {
			username = gc.questionMsg("Enter a nickname for yourself:");
		} while ( "".equals(username) );

		gc.setStatus("Sending nickname to server...");
		
		Map<String, Object> userData = new TreeMap<String, Object>();
		userData.put("status", "username");
		userData.put("username", username);
		
		bayeuxClient.getChannel("/service/user").publish(userData);		
		
	}

	@Override
	public String getSessionIdStr() {		
		return sessionID.toString(16);
	}
	
	@Override
	public BigInteger getSessionBigInt() {		
		return sessionID;
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
	public void processError(String string) {
		gc.blankRedraw("Connection Error");
		gc.setStatus("Connection Error.");
		logger.severe("Connection Error");
		gc.popMsg("There was an error connecting to the server.\n" +
				"Please refresh the page to try again, or return the HIT.");					
		super.disconnect();
	}

	protected void sendBroadcastMsg(Map<String, Object> data) {
		bayeuxClient.getChannel(expBroadcastChan).publish(data);
	}
	
	protected void sendServiceMsg(Map<String, Object> data) {
		bayeuxClient.getChannel(expServiceChan).publish(data);
	}	

	/**
	 * Receive a broadcast message (possibly from another client)
	 * @param clientId
	 * @param data
	 */
	protected abstract void rcvBroadcastMsg(String clientId, Object data);
	
	/**
	 * Receive a service message from server
	 * @param data
	 */
	protected abstract void rcvServiceMsg(Object data);
	
	protected abstract void enableSubmit();

	/* (non-Javadoc)
	 * @see edu.harvard.econcs.turkserver.client.SessionClient#handShakeFail()
	 */
	@Override
	protected void handShakeFail() {		
		gc.blankRedraw("Unable to connect");
		gc.setStatus("Unable to connect to server");
		gc.popMsg(Messages.CONNECT_ERROR, "Connection Error", JOptionPane.ERROR_MESSAGE);	
	}

	protected void runClient() {
							

		// Random error messages for later use

		// Get the cause of the server-side Remote Exception
		Throwable t = new RemoteException().getCause().getCause();		
		gc.blankRedraw("Authentication error");
		gc.setStatus("Session authentication error");

		if( t instanceof SessionUnknownException ) {
			gc.popMsg(Messages.UNRECOGNIZED_SESSION, "Unrecognized Session ID", JOptionPane.ERROR_MESSAGE);
		}
		else if (t instanceof SimultaneousSessionsException) {
			gc.popMsg(Messages.SIMULTANEOUS_SESSIONS, "Too Many Games Open", JOptionPane.WARNING_MESSAGE);
		}
		else if (t instanceof SessionExpiredException ) {
			// TODO this message is the same as above, except on connect
			gc.popMsg(Messages.EXPIRED_SESSION,	"Game Batch Completed", JOptionPane.INFORMATION_MESSAGE);
		}
		else if (t instanceof TooManySessionsException) {
			gc.popMsg(Messages.TOO_MANY_SESSIONS, "Game Limit Hit", JOptionPane.INFORMATION_MESSAGE);
		}
		else if (t instanceof SessionOverlapException ) {
			gc.popMsg(Messages.SESSION_OVERLAP, "Session Already Used", JOptionPane.WARNING_MESSAGE);
		}
		else if (t instanceof SessionCompletedException) {
			gc.popMsg(Messages.SESSION_COMPLETED, "Session Already Completed", JOptionPane.WARNING_MESSAGE);				
			gc.setStatus("Experiment already finished!");				
			enableSubmit();
		} 
		else if (t instanceof QuizFailException) {
			gc.popMsg(Messages.QUIZ_FAILED, "Quiz Failed!", JOptionPane.WARNING_MESSAGE);
			gc.blankRedraw("Quiz Failed!");
			gc.setStatus("Quiz Failed!");
		}
		else if (t instanceof TooManyFailsException) {
			gc.blankRedraw("Too Many Failed Quizzes!");
			gc.popMsg(Messages.TOO_MANY_FAILS, "Too Many Fails!", JOptionPane.WARNING_MESSAGE);
			gc.setStatus("Too many times failed, please come back later!");
		}
		else {
			t.printStackTrace();
			gc.popMsg("Unknown error, please report this and return the HIT, " +
					"or try a different HIT: \n" + t.toString(),
					"Unknown error", JOptionPane.ERROR_MESSAGE);
		}				
		
		// After connect to server, Draw the lobby
		gc.lobbyRedraw();
	
	}
	
	@Override
	protected void connectionClosed() {
		super.connectionClosed();
		
		gc.blankRedraw("Server disconnected");
		gc.setStatus("Server disconnected");
		gc.popMsg(Messages.SERVER_DISCONNECTED,	"Server Disconnected", JOptionPane.WARNING_MESSAGE);
		state = State.DISCONNECTED;
	}
	
}


