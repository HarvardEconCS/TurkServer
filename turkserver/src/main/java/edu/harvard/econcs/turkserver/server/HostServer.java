/**
 * The main server that hosts experiments
 */
package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.*;
import edu.harvard.econcs.turkserver.Codec.LoginStatus;
import edu.harvard.econcs.turkserver.mturk.TurkHITManager;
import edu.harvard.econcs.turkserver.server.ExpServerFactory.ExperimentFactoryException;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ConfigurableServerChannel.Initializer;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerSession;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import net.andrewmao.misc.ConcurrentBooleanCounter;


/**
 * @author Mao
 *
 */
public class HostServer<T extends ExperimentServer<T>> extends SessionServer<BigInteger> {
	
	public static final int ID_BYTES = 20;
	public static final int ID_LEN = ID_BYTES * 8 - 1;	// Number of bits in clientIDs
					
	// Turk crap
	private final TurkHITManager<BigInteger> turkHITManager;
	private final int completedHITGoal;
	
	// Experiment and user information
	private final QuizFactory quizFactory;
	private final ExpServerFactory<T> expFactory;	
	final ExperimentDataTracker tracker;
	
	private final String logPath;
		
	final ConcurrentBooleanCounter<BigInteger> lobbyStatus;	
	
	final AtomicReference<String> serverMessage;
	
	private final AtomicInteger completedHITs;
	
	private LocalSession lobbyBroadcaster;
	
	// Servlet stuff
	private ServletHolder expServlet;

	// GUI
	private ServerFrame serverGUI;
	
	/**
	 * Constructs a server
	 * 
	 * @param factory
	 * @param quizFac
	 * @param userTracker
	 * @param thm
	 * @param completedHitGoal
	 * @param listenPort
	 * @param rmiPort
	 */
	public HostServer(ExpServerFactory<T> factory,
			QuizFactory quizFac,
			ExperimentDataTracker userTracker,
			TurkHITManager<BigInteger> thm,
			Resource[] resources, int completedHitGoal,
			int listenPort, String logPath) {
		// TODO add correct thm, resources, hitGoal to this
		super(userTracker, thm, resources, 0, listenPort);
		
        expServlet = context.addServlet(HostServlet.class, "/exp");  
        expServlet.setInitOrder(3);  
						
		this.quizFactory = quizFac;
		this.expFactory = factory;
		this.tracker = userTracker;
		
		this.turkHITManager = thm;
		this.completedHITGoal = completedHitGoal;				
		
		this.logPath = logPath;
				
		lobbyStatus = new ConcurrentBooleanCounter<BigInteger>();				
		
		completedHITs = new AtomicInteger(0);
		serverMessage = new AtomicReference<String>("");
		
		serverGUI = new ServerFrame(this);
				
	}

	/**
	 * Constructs a server with the default port
	 * 
	 * @param factory
	 * @param userTracker
	 * @param thm
	 * @param completedHitGoal
	 * @param listenPort
	 */
	public HostServer(ExpServerFactory<T> factory, ExperimentDataTracker userTracker,
			TurkHITManager<BigInteger> thm, int completedHitGoal,
			int listenPort, String logPath) {
		this(factory, null, userTracker, thm, null, completedHitGoal, listenPort, logPath); 
	}	
	
	public String getLogPath() {		
		return logPath;
	}

	@Override
	protected LoginStatus sessionAccept(String clientId, BigInteger hitId,
			String assignmentId, String workerId) {		
		LoginStatus ls = super.sessionAccept(clientId, hitId, assignmentId, workerId);
		
		Map<String, Object> data = new HashMap<String, Object>();
		ServerSession session = bayeux.getSession(clientId);
				
		if( ls == LoginStatus.QUIZ_REQUIRED ) {
			// Check if we need to do a quiz
			QuizMaterials qm = quizFactory.getQuiz();
			if( qm != null ) {
							
				data.put("status", Codec.quizNeeded);
				data.put("quiz", qm.toData());
				
				session.deliver(session, "/service/user", data, null);				
			}
		}
		else if( ls == LoginStatus.NEW_USER ) {
			// Check if we need to get username			
			data.put("status", Codec.usernameNeeded);
			
			session.deliver(session, "/service/user", data, null);
		}		
		else if( ls == LoginStatus.REGISTERED ) {			
			/* Is user already in an experiment?
			 * already-completed sessions should be caught way before this
			 */
			if( tracker.sessionIsInProgress(hitId) ) {
				ExperimentServer<?> exp = tracker.getExperimentForID(hitId);
				if( exp != null ) {
					data.put("status", Codec.connectExpAck);
					data.put("channel", exp.getChannelName());

					session.deliver(session, "/service/user", data, null);

					// TODO experiment should send state to user (?)
					exp.clientConnected(hitId);				
				}
				else {
					logger.severe("Unknown experiment but session is in progress: "	+ 
							hitId.toString(16) );
				}
			} 
			else {
				data.put("status", Codec.connectLobbyAck);				
				session.deliver(session, "/service/user", data, null);
				
				logger.info(String.format("%s (%s) connected to lobby",	
						hitId.toString(16), tracker.getUsername(hitId)));
			}								
			
		}
		else {
			logger.warning("Unexpected id tried to connect: " + hitId.toString(16));
			data.put("status", Codec.connectErrorAck);				
			session.deliver(session, "/service/user", data, null);
		}
		
		return ls;
	}

	//	@Override
	public QuizMaterials getQuizMaterials(BigInteger sessionID,
			String assignmentId, String workerId) {		
		if( quizFactory != null ) 
			return quizFactory.getQuiz();
		else
			return null;
	}

//	@Override
	public void sendQuizResults(BigInteger sessionID, QuizResults qr) {		
		try {
			tracker.saveQuizResults(sessionID, qr);
			
			/* TODO quiz passed? allow lobby login
			 * careful - this assumes that quiz always precedes username
			 */
			sendStatus(sessionID, "username");
			
		} catch (QuizFailException e) {
			sendStatus(sessionID, "quizfail");
		}		
	}

	public void sendStatus(BigInteger sessionID, String status) {
		Map<String, Object> data = new HashMap<String, Object>();		
		ServerSession session = bayeux.getSession(clientToId.inverse().get(sessionID));
		data.put("status", status);		
		session.deliver(session, "/service/user", data, null);
	}

	public boolean lobbyLogin(BigInteger sessionID, String username) {		
		if( ! tracker.lobbyLogin(sessionID, username) )  {
			// some error, request it again.
			sendStatus(sessionID, "username");
			
			// Don't broadcast this username
			return false;
		}
		return true;
	}

	public boolean lobbyUpdate(BigInteger sessionID, boolean isReady) {
		lobbyStatus.put(sessionID, isReady);				
		
		int neededPeople = expFactory.getExperimentSize();
		
		// are there enough people ready to start?
		synchronized(lobbyStatus) {	
			logger.info("Lobby has " + lobbyStatus.getTrueCount() + " ready people");				
			if( lobbyStatus.getTrueCount() >= neededPeople ) {
				
				// Create a new experiment and assign the ready people to it
				createNewExperiment();
				
			}
			else if( lobbyStatus.size() < neededPeople ) {
				
				// Make sure everyone's ready is disabled
				for( BigInteger id : lobbyStatus.keySet() ) {
					lobbyStatus.put(id, false);
				}
			}
		}
							
		serverGUI.updateLobbyModel();
		
		// Notify everyone who is remaining in the lobby
		sendLobbyStatus();
		
		return true;
	}

	void sendLobbyStatus() {
		Map<String, Object> data = new TreeMap<String, Object>();
		
		int usersInLobby = lobbyStatus.size();
		int usersNeeded = expFactory.getExperimentSize();
		
		data.put("status", "update");
		
		data.put("numusers", usersInLobby);
		data.put("numneeded", usersNeeded);
		data.put("joinenabled", usersInLobby >= usersNeeded);
		
		data.put("servermsg", serverMessage.get());
		data.put("currentexps", tracker.getNumExpsRunning());
		data.put("totalusers", bayeux.getSessions().size());
		
		// TODO could be some race conditions here if lobby size changes?
		Object[] users = new Object[lobbyStatus.size()];		
		int i = 0;
		for( Map.Entry<BigInteger, Boolean> e : lobbyStatus.entrySet() ) {
			BigInteger id = e.getKey();
			
			// clientId, username, and status
			users[i++]= new Object[] { clientToId.inverse().get(id), tracker.getUsername(id), e.getValue() };
		}
		data.put("users", users);
		
		// TODO broadcast to lobby?
		bayeux.getChannel("lobby").publish(this.lobbyBroadcaster, data, null);
		
	}

	private void createNewExperiment() {
		int expSize = expFactory.getExperimentSize();
		ConcurrentHashMap<BigInteger, Boolean> expClients = 
			new ConcurrentHashMap<BigInteger, Boolean>(expSize);
		
		// Count up exactly expSize people for the new experiment
		int counter = 0;
		for( Map.Entry<BigInteger, Boolean> e : lobbyStatus.entrySet() ) {						
			if( e.getValue() == true ) {							
				expClients.put(e.getKey(), e.getValue());
				counter++;
			}
			
			// Don't put more than the required number of people, even if more are ready
			if(counter == expSize) break;
		}					
		
		T newExp = null;
		try {
			newExp = expFactory.getNewExperiment(this, expClients);															
		} catch (ExperimentFactoryException e) {
			/* Something wrong with generating the experiment, 
			 * put everyone back in the lobby as not ready
			 * and send an error message
			 */
			e.printStackTrace();
			for( BigInteger id : expClients.keySet()) {
				lobbyStatus.put(id, false);
				sendStatus(id, Codec.startExpError);
			}
			return;
		}
		
		/* Create necessary channels for this experiment
		 * Note that hostServlet automatically routes these already
		 */
		Initializer persistent = new Initializer() {
			@Override
			public void configureChannel(ConfigurableServerChannel channel) {
				channel.setPersistent(true);
			}			
		};
		
		bayeux.createIfAbsent(Codec.expChanPrefix + newExp.getChannelName(), persistent);
		bayeux.createIfAbsent(Codec.expSvcPrefix + newExp.getChannelName(), persistent);
		
		serverGUI.newExperiment(newExp);	
		
		/* Update tracking information for experiment
		 * TODO does this start directing requests to the server before it's started?
		 */
		tracker.newExperimentStarted(newExp);
		
		// Starting the experiment sends out the appropriate notifications to clients
		new Thread(newExp, newExp.toString()).start();
		
		/* No problem in starting the exp - now can remove from lobby
		 * everyone in the new exp is removed before lobby is notified 
		 * due to synchronize over lobbyStatus above
		 * 
		 * Moved this down here to try and avoid race condition? 
		 * So that they are in experiment before being out of lobby - limbo state 
		 * since experiment is checked first
		 */			
		for( BigInteger id : expClients.keySet()) lobbyStatus.remove(id);
	}

	public void addInactiveTime(BigInteger sessionId, long inactiveTime) {		
		ExperimentServer<?> exp = tracker.getExperimentForID(sessionId);
		exp.addInactiveTime(sessionId, inactiveTime);		
	}

	public void experimentFinished(T expServer) {
		tracker.experimentFinished(expServer);		
		serverGUI.finishedExperiment(expServer);				
		
		// unsubscribe from and/or remove channels
		ServerChannel toRemove = null;
		if( (toRemove = bayeux.getChannel(Codec.expChanPrefix + expServer.getChannelName())) != null ) {
			toRemove.setPersistent(false);
		}
		if( (toRemove = bayeux.getChannel(Codec.expSvcPrefix + expServer.getChannelName())) != null ) {
			toRemove.setPersistent(false);
		}
		
		// Tell clients they are done!
		for( BigInteger id : expServer.clients.keySet() )
			sendStatus(id, Codec.doneExpMsg);	
		
		if( completedHITs.addAndGet(expServer.clients.keySet().size()) >= completedHITGoal ) {
			logger.info("Goal of " + completedHITGoal + " users reached!");
			
			if( turkHITManager != null ) turkHITManager.expireRemainingHITs();
			
			// Only notify people in experiment, not lobby (experiment people need to submit)
			for( BigInteger id : lobbyStatus.keySet() )
				sendStatus(id, Codec.batchFinishedMsg);
			
			// TODO quit the thread in this case
		}
		
		
	}
	
	protected void runServerInit() {
		expFactory.doInit(this);
		
		// Start the generating factory if it needs to do work
		// TODO have this quit properly
		new Thread(expFactory, expFactory.getClass().getSimpleName()).start();				
		
		lobbyBroadcaster = bayeux.newLocalSession("lobby");
		lobbyBroadcaster.handshake();
		
		
	}
	
	@Override
	public void sessionDisconnect(String clientId, BigInteger sessionId) {
		
		// Was this dude in the lobby? If so remove him from the lobby and notify lobby ppl		
		if( lobbyStatus.remove(sessionId) != null ) {			
			logger.info(String.format("%s (%s) removed from lobby",
					sessionId.toString(16), tracker.getUsername(sessionId)));			
			
			// TODO check on this quit message to lobby
			Map<String, Object> data = new TreeMap<String, Object>();
			
			data.put("status", "quit");
			data.put("username", tracker.getUsername(sessionId));
			
			bayeux.getChannel("/lobby").publish(bayeux.getSession(clientId), data, null);
		}
		
		// This takes care of disconnecting in the tracker
		super.sessionDisconnect(clientId, sessionId);
	}

	public class UsernameComparator implements Comparator<BigInteger> {	
		@Override
		public int compare(BigInteger o1, BigInteger o2) {
			String u1 = tracker.getUsername(o1);
			String u2 = tracker.getUsername(o2);

			if( u1 != null ) {
				int comp = u1.compareTo(u2);
				if( comp != 0 ) return comp;				
			}
			
			return o1.compareTo(o2);
		}	
	}

	@Override
	public BigInteger stringToType(String sessionId) {		
		return new BigInteger(sessionId, 16);
	}

	/**
	 * 
	 * @param clientId
	 * @param data
	 * @return
	 */
	void experimentServiceMsg(String clientId, Map<String, Object> data) {		
		BigInteger sessionId = getSessionForClient(clientId);
		if( sessionId == null ) {
			logger.warning("Message from unrecognized client: " + clientId);
		}
		else {
			@SuppressWarnings("unchecked")
			T exp = (T) tracker.getExperimentForID(sessionId);
			
			if( exp == null ) {
				logger.warning("No experiment recorded for client: " + clientId);
				return;
			}
			
			exp.rcvServiceMsg(sessionId, data);						
		}		
	}
	
	/**
	 * Deliver a message to an experiment. Can be broadcast (automatically routed to other clients) or private 
	 * @param clientId
	 * @param data 
	 */
	boolean experimentBroadcastMsg(String clientId, Map<String, Object> data) {		
		BigInteger sessionId = getSessionForClient(clientId);
		if( sessionId == null ) {
			logger.warning("Message from unrecognized client: " + clientId);
			return false;
		}
		else {
			@SuppressWarnings("unchecked")
			T exp = (T) tracker.getExperimentForID(sessionId);
			
			if( exp == null ) {
				logger.warning("No experiment recorded for client: " + clientId);
				return false;
			}
						
			return exp.rcvBroadcastMsg(sessionId, data);			
		}		
	}

}
