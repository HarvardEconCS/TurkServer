package edu.harvard.econcs.turkserver.client;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.cometd.annotation.*;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.client.BayeuxClient;
import org.cometd.client.ext.AckExtension;
import org.cometd.client.ext.TimesyncClientExtension;
import org.cometd.client.transport.LongPollingTransport;

public abstract class SessionClient<T> implements Runnable {
	
	protected final Logger logger;
	
	private final String url;
	protected final T hitId;
	protected final String assignmentId;
	protected final String workerId;
	
	protected BayeuxClient bayeuxClient;
	protected ClientAnnotationProcessor processor;
	
	protected volatile boolean connected;
	private volatile boolean wasConnected;
	
	private volatile boolean isError = false;
	
	protected SessionClient(String cometURL, T hitId, String assignmentId, String workerId) {
		this.url = cometURL;
		this.hitId = hitId;
		this.assignmentId = assignmentId;
		this.workerId = workerId;
		
		bayeuxClient = new BayeuxClient(url, LongPollingTransport.create(null));
		
		bayeuxClient.addExtension(new TimesyncClientExtension());
		bayeuxClient.addExtension(new AckExtension());
	
		logger = Logger.getLogger(this.getClass().getSimpleName() + this.getSessionID());
		
		processor = new ClientAnnotationProcessor(bayeuxClient);
		
		processor.process(new UserClientService());
	}
	
	public void submit() {
		Map<String, Object> m = new HashMap<String, Object>();
		
		m.put("status", "submit");
		m.put("hitId", hitId);
		m.put("workerId", workerId);
		
		bayeuxClient.getChannel("/service/user").publish(m);		
	}	
	
	public void disconnect() {
		bayeuxClient.getChannel("/service/user").unsubscribe();
		
		bayeuxClient.disconnect();
	}
	
	public BayeuxClient getBayeux() { return bayeuxClient; }
	
	public boolean getIsError() { return isError; }		
	
	/**
	 * @return the hitId
	 */
	public abstract String getSessionID();

	/**
	 * @return the assignmentId
	 */
	public String getAssignmentId() { return assignmentId; }

	/**
	 * @return the workerId
	 */
	public String getWorkerId() { return workerId; }

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
			data.put("status", "accept");
			data.put("hitId",  hitId);
			data.put("assignmentId", assignmentId);
			data.put("workerId", workerId);
			
			System.out.println(bayeuxClient.getId() + " sending accept");
		}
		else {
			data.put("status", "view");
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
	
	@Service
	public class UserClientService {
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
			}
			else {
				System.out.println("Handshake failed.");
			}
		}
		
		@Listener(Channel.META_DISCONNECT)
		public void metaDisconnect(Message disconnect) {
			if( disconnect.isSuccessful() ) {
				connected = false;
			}			
		}
		
		@Subscription("/service/user")
		public void serviceUser(Message service) {
			Map<String, Object> m = service.getDataAsMap();
			Object status = m.get("status");
			if( status != null ) {
				System.out.println("Status: " + status.toString() + ", " + "Message: " + m.get("msg"));
				
				if( "error".equals(status.toString()) ) {
					isError = true;
					processError(m.get("msg").toString());
				}
				else if( "completed".equals(status.toString()) ) {
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

	public abstract void processError(String string);

	@Override
	public final void run() {		
		Thread.currentThread().setName(this.getClass().getSimpleName() + " " + getSessionID());
		logger.info("Begin client log with session ID " + getSessionID());
		
		bayeuxClient.handshake();
		boolean success = bayeuxClient.waitFor(1000, BayeuxClient.State.CONNECTED);
		
		if( !success ) {
			handShakeFail();			
			return;
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {								
				logger.info("Disconnecting Bayeux client");				
				disconnect();
			}
		});
	}

	/* ****************************************************
	 * Various methods that can be overridden by the client
	 ******************************************************/
	protected void handShakeFail() {
		System.out.println("Failed Handshake. Quitting.");
	}
	
	protected void runClient() {}
	
}
