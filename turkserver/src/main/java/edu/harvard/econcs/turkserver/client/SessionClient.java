package edu.harvard.econcs.turkserver.client;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.cometd.annotation.*;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;
import org.cometd.client.ext.AckExtension;
import org.cometd.client.ext.TimesyncClientExtension;
import org.cometd.websocket.client.WebSocketTransport;

import org.eclipse.jetty.websocket.WebSocketClientFactory;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.api.ClientController;

public class SessionClient<C> implements ClientController {
	
	static final WebSocketClientFactory wsFactory = new WebSocketClientFactory();
	
	protected Logger logger;
	
	private String hitId;
	private String assignmentId;
	private String workerId;
	
	protected String expBroadcastChan;
	protected String expServiceChan;
	
	private MessageListener broadcastListener = null;
	private MessageListener serviceListener = null;
	
	protected ClientAnnotationManager<C> clientWrapper;
	
	protected BayeuxClient bayeuxClient;	
	protected ClientAnnotationProcessor processor;
	private UserClientService ucl;
		
	private volatile boolean connected;
	private volatile boolean wasConnected;
	private volatile boolean isError = false;
	
	protected SessionClient() {				

	}	

	public C getClientBean() {
		return clientWrapper.clientBean;
	}
	
	/* ********************************************
	 * ClientController methods
	 **********************************************/
	
	@Override
	public String getHitId() { return hitId; }


	@Override
	public String getAssignmentId() { return assignmentId; }


	@Override
	public String getWorkerId() { return workerId; }

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() { return bayeuxClient == null ? connected : bayeuxClient.isConnected(); }

	public boolean isError() { return isError; }
	
	public void connect(String url, String hitId, String assignmentId, String workerId) {
		if( bayeuxClient != null ) throw new RuntimeException("Already attempted a connect!");
				
		this.hitId = hitId;
		this.assignmentId = assignmentId;
		this.workerId = workerId;
		
		logger = Logger.getLogger(this.getClass().getSimpleName() + this.getHitId());
				
//		LongPollingTransport.create(null)		
		bayeuxClient = new BayeuxClient(url, WebSocketTransport.create(null, wsFactory));		
		
		bayeuxClient.addExtension(new TimesyncClientExtension());
		bayeuxClient.addExtension(new AckExtension());
					
		processor = new ClientAnnotationProcessor(bayeuxClient);		
		processor.process(ucl = new UserClientService());
				
		logger.info("Attempting connection with ID " + getHitId());		
		bayeuxClient.handshake();
		
		// Start a thread that will watch for connection success or failure
		new Thread() {
			public void run() {						
				if( !bayeuxClient.waitFor(2000, BayeuxClient.State.CONNECTED) ) {
					handShakeFail();
					return;
				}								
			}			
		}.start();
	}

	public void disconnect() {
		if( bayeuxClient == null || !bayeuxClient.isConnected() ) return;
		
		// unsubscribe from any experiment channel too
		if( broadcastListener != null ) {
			bayeuxClient.getChannel(expBroadcastChan).unsubscribe(broadcastListener);
			broadcastListener = null;
		}
		if( serviceListener != null ) {
			bayeuxClient.getChannel(expServiceChan).removeListener(serviceListener);
			serviceListener = null;
		}
		
		processor.deprocess(ucl);
		
		try {
			bayeuxClient.disconnect();
		}
		catch( Exception e ) {
			logger.info("Ignoring thrown exception on disconnect attempt");
		}
	}

	@Override
	public void sendQuizResults(QuizResults qr) {		
		Map<String, Object> quizResults = new TreeMap<String, Object>();
		quizResults.put("status", "quizresults");
		quizResults.put("correct", qr.correct);
		quizResults.put("total", qr.total);
		
		bayeuxClient.getChannel("/service/user").publish(quizResults);			
	}

	@Override
	public void sendExperimentBroadcast(Map<String, Object> data) {
		bayeuxClient.getChannel(expBroadcastChan).publish(data);
	}
	
	@Override
	public void sendExperimentService(Map<String, Object> data) {
		bayeuxClient.getChannel(expServiceChan).publish(data);
	}	

	public void subscribeExpChannel(String chan) {
		expBroadcastChan = Codec.expChanPrefix + chan;
		logger.info("Subscribing to exp broadcast channel: " + expBroadcastChan);
		expServiceChan = Codec.expSvcPrefix + chan;
		logger.info("Subscribing to exp service channel: " + expServiceChan);
		
		broadcastListener = new MessageListener() {
			@Override
			public void onMessage(ClientSessionChannel channel, Message message) {				
				clientWrapper.deliverBroadcast(message.getDataAsMap());
			}
		};						
		
		serviceListener = new MessageListener() {
			@Override
			public void onMessage(ClientSessionChannel channel, Message message) {							
				clientWrapper.deliverService(message.getDataAsMap());
			}
		};
		
		bayeuxClient.getChannel(expBroadcastChan).subscribe(broadcastListener);
		// Don't use addListener here...gives random useless messages
		bayeuxClient.getChannel(expServiceChan).subscribe(serviceListener);		
	}
	
	@Service
	private class UserClientService {
		@Session
		private ClientSession client;
		
		@Listener(Channel.META_CONNECT)
		public void metaConnect(Message connectMsg) {
	        if (bayeuxClient.isDisconnected())
	        {
	            connected = false;
	            connectionClosed();
	            return;
	        }
	
	        wasConnected = connected;
	        connected = connectMsg.isSuccessful();
	        
	        if (!wasConnected && connected)
	        {
	            connectionEstablished();
	        }
	        else if (wasConnected && !connected)
	        {
	            connectionBroken();
	        }
		}
		
		@Listener(Channel.META_HANDSHAKE)
		public void metaHandshake(Message handshake) {
			if( handshake.isSuccessful() ) {
				connectionInitialized();
				
				// Succeeded...just add a disconnect hook.
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {								
						logger.info("Disconnecting Bayeux client");				
						disconnect();
					}
				});
			}
			else {
				handShakeFail();
			}
		}
		
		@Listener(Channel.META_DISCONNECT)
		public void metaDisconnect(Message disconnect) {
			// TODO: this doesn't seem to be working with websocket
			if( disconnect.isSuccessful() ) {				
				connected = false;
			}			
		}
		
		@Subscription("/service/user")
		public void serviceUser(Message service) {
			Map<String, Object> m = service.getDataAsMap();
			Object status = m.get("status");
			
			if( status != null ) {										
				if( Codec.status_expfinished.equals(status.toString() ) ) {
					logger.info("Connected to experiment that is already done");
					clientWrapper.triggerClientError(Codec.status_expfinished);														
					disconnect();
				}
				else if( Codec.status_connectexp.equals(status.toString())) {					
					// Subscribe to this channel
					String chan = m.get("channel").toString();					
					subscribeExpChannel(chan);					
					
					clientWrapper.triggerStartExperiment();
				}
				else if( Codec.roundStartMsg.equals(status.toString())) {
					clientWrapper.triggerStartRound(((Number) m.get("round")).intValue());
				}
				else if( Codec.doneExpMsg.equals(status.toString())) {
					clientWrapper.triggerFinishExperiment();			
					// Do nothing
				} 
				else if( Codec.status_batchfinished.equals(status.toString())) {
					clientWrapper.triggerClientError(Codec.status_batchfinished);					
					disconnect();					
				}
				else if( Codec.status_error.equals(status.toString()) ) {					
					clientWrapper.triggerClientError(m.get("msg").toString());
					isError = true;
				}
				else if( Codec.status_completed.equals(status.toString()) ) {
					System.out.println("Got complete confirmation, disconnecting.");
					disconnect();
				}
			}
			else {
				System.out.println("Service message unexpected:");
				System.out.println(m);
			}
			
		}
	}

	/* ****************************************************
	 * Connection status handlers, can be overridden by subclasses
	 ******************************************************/
	protected void handShakeFail() {
		System.out.println("BayeuxClient Failed Handshake.");
	}

	/**
	 * First time connection is initialized
	 */
	protected void connectionInitialized() {
		System.out.println(bayeuxClient.getId() + " initialized");
	}

	/**
	 * Connection established, could be after first time
	 */
	protected void connectionEstablished() {
		System.out.println(bayeuxClient.getId() + " established");
		
		Map<String, Object> data = new HashMap<String, Object>();
		
		if( assignmentId != null && !assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE") ) {
			data.put("status", Codec.hitAccept);
			data.put("hitId",  hitId);
			data.put("assignmentId", assignmentId);
			data.put("workerId", workerId);
			
			System.out.println(bayeuxClient.getId() + " sending accept");
		}
		else {
			data.put("status", Codec.hitView);
			data.put("hitId",  hitId);
			
			System.out.println(bayeuxClient.getId() + " sending view");
		}
		
		bayeuxClient.getChannel("/service/user").publish(data);
	}

	/**
	 * Connection broken
	 */
	protected void connectionBroken() {
				
	}

	/**
	 * Disconnected (either by server or by client)
	 */
	protected void connectionClosed() {
		
	}

	public void submit(String comments) {
		Map<String, Object> m = new HashMap<String, Object>();
		
		m.put("status", Codec.hitSubmit);
		m.put("comments", comments);
		
		bayeuxClient.getChannel("/service/user").publish(m);
	}

	@Override
	public void recordInactivity(long timeInactive) {
		// TODO Auto-generated method stub
		
	}	
	
}
