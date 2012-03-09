/**
 * The main server that hosts experiments
 */
package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.*;
import edu.harvard.econcs.turkserver.Codec.LoginStatus;
import edu.harvard.econcs.turkserver.Update.*;
import edu.harvard.econcs.turkserver.mturk.TurkHITManager;
import edu.harvard.econcs.turkserver.server.ExpServerFactory.ExperimentFactoryException;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import net.andrewmao.misc.ConcurrentBooleanCounter;


/**
 * @author Mao
 *
 */
public class HostServer<T extends ExperimentServer<T>> implements Runnable, Updater {
	
	public static final int ID_BYTES = 20;
	public static final int ID_LEN = ID_BYTES * 8 - 1;	// Number of bits in clientIDs
	public static final int BUF_LEN = 4096; 		// Fixed buffer size
		
	private final Logger logger = Logger.getLogger(HostServer.class.getSimpleName());
	
	// RMI stuff
	private final int listenPort;
	private final int rmiPort;		
	private Updater stub;
	private Registry reg;

	// NIO stuff
	private final CharsetEncoder encoder = Codec.getEncoder();
	private ByteBuffer buffer = ByteBuffer.allocateDirect(HostServer.BUF_LEN);	
	
	private ServerSocketChannel ssc;
	private Selector sel;
	private SelectionKey serverKey;
	
	// Turk crap
	private final TurkHITManager<BigInteger> turkHITManager;
	private final int completedHITGoal;
	
	// Experiment and user information
	private final QuizFactory quizFactory;
	private final ExpServerFactory<T> expFactory;	
	final ExperimentDataTracker tracker;
	
	private final String logPath;
	
	// Client trackers
	private final ConcurrentHashMap<BigInteger, SocketChannel> idToChan;
	private final ConcurrentHashMap<SocketChannel, BigInteger> chanToID;
	
	final ConcurrentBooleanCounter<BigInteger> lobbyStatus;
	final ConcurrentHashMap<BigInteger, String> lobbyMessage;
	
	final AtomicReference<String> serverMessage;
	
	private final AtomicInteger completedHITs;
	
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
			TurkHITManager<BigInteger> thm, int completedHitGoal,
			int listenPort, int rmiPort, String logPath) {
						
		this.quizFactory = quizFac;
		this.expFactory = factory;
		this.tracker = userTracker;
		
		this.turkHITManager = thm;
		this.completedHITGoal = completedHitGoal;
		
		this.listenPort = listenPort;
		this.rmiPort = rmiPort;
		
		this.logPath = logPath;
		
		idToChan = new ConcurrentHashMap<BigInteger, SocketChannel>();		
		chanToID = new ConcurrentHashMap<SocketChannel, BigInteger>();
		
		lobbyStatus = new ConcurrentBooleanCounter<BigInteger>();		
		lobbyMessage = new ConcurrentHashMap<BigInteger, String>();
		
		completedHITs = new AtomicInteger(0);
		serverMessage = new AtomicReference<String>("");
		
		serverGUI = new ServerFrame(this);
	}

	/**
	 * Constructs a server with the default RMI port
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
		this(factory, null, userTracker, thm, completedHitGoal, listenPort, 0, logPath); 
	}
	
	public String getLogPath() {		
		return logPath;
	}

	@Override
	public LoginStatus sessionLogin(BigInteger sessionID, String assignmentId, String workerId)
	throws RemoteException {		
		try {
			return tracker.registerAssignment(sessionID, assignmentId, workerId);
		} catch (ExpServerException e) {			
			throw new RemoteException("Server Error:", e);
		}					
	}

	@Override
	public QuizMaterials getQuizMaterials(BigInteger sessionID,
			String assignmentId, String workerId) {		
		if( quizFactory != null ) 
			return quizFactory.getQuiz();
		else
			return null;
	}

	@Override
	public void sendQuizResults(BigInteger sessionID,
			String assignmentId, String workerId, QuizResults qr) throws RemoteException {
		try {
			tracker.saveQuizResults(sessionID, workerId, qr);
		} catch (QuizFailException e) {
			throw new RemoteException("Server Error:", e);
		}
	}

	@Override
	public boolean lobbyLogin(BigInteger sessionID, String username) throws RemoteException {		
		return tracker.lobbyLogin(sessionID, username);
	}

	@Override
	public boolean clientUpdate(CliUpdate u) throws RemoteException {
		// If someone is in an experiment, they shouldn't be sending lobby updates
		ExperimentServer<?> exp = tracker.getExperimentForID(u.clientID);
		
		if( exp != null ) {
			exp.receiveUpdate(u);
			return true;
		} 		
		else if (u instanceof LobbyStatusUpdate) {
			
			LobbyStatusUpdate lsu = (LobbyStatusUpdate) u;			
			lobbyStatus.put(lsu.clientID, lsu.isReady);
			if( lsu.msg != null ) lobbyMessage.put(lsu.clientID, lsu.msg);
			
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
				
			// Notify everyone who is remaining in the lobby
			sendLobbyStatus();
			
			return true;
		}
		else {
			return false;
		}
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
				notifyClient(id, Codec.startExpError);
			}
			return;
		}
		
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

	protected void sendLobbyStatus() {
		for( BigInteger lobbyDude : lobbyStatus.keySet() ) {
			notifyClient(lobbyDude, Codec.lobbyUpdateMsg);
		}
		serverGUI.updateLobbyModel();
	}

	public SrvUpdate pullUpdate(UpdateReq clientReq) throws RemoteException {
		ExperimentServer<?> exp = tracker.getExperimentForID(clientReq.clientID);
		
		LobbyUpdateReq lobbyur = null;
		if( clientReq instanceof LobbyUpdateReq )
			lobbyur = (LobbyUpdateReq) clientReq;
				
		if( exp != null ) {
			if( lobbyur != null ) {
				logger.warning("Client " + clientReq.clientID.toString(16) + 
				" asked for a lobby update but should already be in experiment");
			}
			
			// Ask experiment server for update								
			try {
				return exp.getUpdate(clientReq);
			} catch (ExpServerException e) {
				throw new RemoteException("Experiment server exception", e);
			}
		} 
		else if (lobbyur != null) {			
			// Populate lobby update
			int usersInLobby = lobbyStatus.size();
			int usersNeeded = expFactory.getExperimentSize();			
			boolean joinEnabled = (lobbyStatus.size() >= usersNeeded);					
			
			LobbyUpdateResp resp = 
				new LobbyUpdateResp(lobbyur.clientID, usersNeeded, joinEnabled, usersInLobby,
						serverMessage.get(), tracker.getNumExpsRunning(), chanToID.size());
			
			for( Map.Entry<BigInteger, Boolean> e : lobbyStatus.entrySet() ) {
				BigInteger id = e.getKey();
				String msg = lobbyMessage.get(id);
				if( msg == null ) msg = "";
				
				resp.addRecord(id, tracker.getUsername(id), e.getValue(), msg);
			}
			return resp;
		}
		else {
			logger.warning("Got a client request that is not in the lobby or experiment!");
			throw new RemoteException("Unexpected client request: " + clientReq.getClass().getSimpleName());
		}
	}

	/**
	 * Note: this is called asynchronously by worker threads from client RMI
	 * TODO possibly unify the notifying system
	 */
	public void notifyClient(BigInteger id, String msg) {
		SocketChannel sc = idToChan.get(id);
		
		if( sc == null || sc.socket().isClosed() ) return;
		
		try {
			// put whitespace here so client can split multiple messages
			sc.write(encoder.encode(CharBuffer.wrap(msg + "\r\n")));
		} catch (CharacterCodingException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		}		
	}	
		
	public void experimentFinished(T expServer) {
		tracker.experimentFinished(expServer);		
		serverGUI.finishedExperiment(expServer);				
		
		// Tell clients they are done!
		for( BigInteger id : expServer.clients.keySet() )
			notifyClient(id, Codec.doneExpMsg);	
		
		if( completedHITs.addAndGet(expServer.clients.keySet().size()) >= completedHITGoal ) {
			logger.info("Goal of " + completedHITGoal + " users reached!");
			
			if( turkHITManager != null ) turkHITManager.expireRemainingHITs();
			
			// Only notify people in experiment, not lobby (experiment people need to submit)
			for( BigInteger id : lobbyStatus.keySet() )
				notifyClient(id, Codec.batchFinishedMsg);
			
			// TODO quit the thread in this case
		}				
	}

	/*
	 * Main thread
	 */	
	public void run() {
		Thread.currentThread().setName("HostServer");
		
		// Start the generating factory if it needs to do work
		// TODO have this quit properly
		new Thread(expFactory, expFactory.getClass().getSimpleName()).start();
		
		// Register the RMI stub
		try {
			stub = (Updater) UnicastRemoteObject.exportObject(this, rmiPort);
			
			reg = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
			reg.rebind(Updater.class.getSimpleName(), stub);
			
			logger.info("RMI registry created");
		} catch (RemoteException e) {
			logger.severe("Failed to bind RMI server");			
			e.printStackTrace();
			return;
		} 
		
		// Start the nonblocking I/O thread
		try {
			sel = SelectorProvider.provider().openSelector();
			
			ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);			
			
			// Listen on all interfaces (more likely to work than trying to find IP)
			InetSocketAddress isa = new InetSocketAddress("0.0.0.0", listenPort);
			ssc.socket().bind(isa);
			
			serverKey = ssc.register(sel, SelectionKey.OP_ACCEPT);
			logger.info("Listening on port " + listenPort);
		} catch (IOException e) {
			logger.severe("Could not open server port");
			e.printStackTrace();
			return;
		}
		
		// Post Turk HITs
		if( turkHITManager != null ) new Thread(turkHITManager).start();
		
		// Register shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				// TODO add stuff from above to in here, like auto expiring hits
				
				logger.info("Shutdown initiated");
				
				try {
					sel.close();
					ssc.close();
					for( SocketChannel sc : chanToID.keySet() ) {
						sc.close();
					}
				} catch (IOException e) {					
					e.printStackTrace();
				}
			}
		});			
		
		// Enter main loop
		logger.info("Waiting for clients on " + listenPort);
		while( true  ) {
			Iterator<SelectionKey> it = null;
			
			try {
				if (sel.select() == 0) continue;
				it = sel.selectedKeys().iterator();
			} catch (ClosedSelectorException e) {
				// Shutdown has been initiated
				break;
			} catch (IOException e) {
				e.printStackTrace();
				if (it == null) continue;
			}
			
			while(it.hasNext()) {
				SelectionKey sk = it.next();
				it.remove();

				if( sk == serverKey && sk.isAcceptable() ) {					
					// new connection (could be a new client or reconnecting client)					
					try {
						SocketChannel sc;
						sc = ((ServerSocketChannel) sk.channel()).accept();
						sc.configureBlocking(false);
						

						// add this to the selector for reading
						sc.register(sel, SelectionKey.OP_READ);
					} catch (IOException e) {						
						e.printStackTrace();
					}										
				} else {
					// Someone sent some data
					buffer.clear();
					SocketChannel sc = (SocketChannel) sk.channel();
										
					int bytesRead = 0;					
					try {
						// Read the message into buffer
						bytesRead = sc.read(buffer);												
						logger.fine(bytesRead + " bytes read");
						
						if( bytesRead == -1 ) {
							// Stream closed
							sc.close();
							removeUserData(sc);																			
							continue;
						}
						
					} catch (IOException e1) {
						/* TODO this catches "Connection reset by peer or connection timed out",
						 * but careful if there is anything else
						 */
						e1.printStackTrace();
						logger.warning("Assuming that this guy disconnected:");
						try{ sc.close(); } catch(IOException e2) { e2.printStackTrace(); }
						removeUserData(sc);
						continue;
					} 														
					
					buffer.flip();

					if( chanToID.containsKey(sc) && !sc.socket().isClosed() ) {						
						/* TODO Message sent on open channel, not really used right now
						 * And does not take into account lobby
						 */
						
						BigInteger id = chanToID.get(sc);
						ExperimentServer<?> exp = tracker.getExperimentForID(id);
						
						if (exp != null) exp.processData(buffer, bytesRead);
						else { logger.warning("Unknown experiment for " + id.toString(16)); }
					}
					else {
						// Message sent on unrecognized channel
						processNewChannel(sc, bytesRead);
					}						
				}						
			}
		}		
	}

	private void processNewChannel(SocketChannel sc, int bytesRead) {
		// Check their ID to see if this is their first time connecting		 
		
		byte[] idbuf = new byte[bytesRead];
		buffer.get(idbuf, 0, bytesRead);
		BigInteger id = new BigInteger(idbuf);		
		
		if( idToChan.containsKey(id) ) {
			// Close any old channel associated with this id, if one exists (double connect)
			SocketChannel oldChannel = idToChan.remove(id);
			try {
				oldChannel.close();
			} catch( IOException e ) {
				e.printStackTrace();
			}
		}
		
		try {
			if( tracker.sessionIsInProgress(id) ) {								
				// Send reply - reconnecting to an experiment								 

				sc.write(encoder.encode(CharBuffer.wrap(Codec.reconnectExpAck)));

				chanToID.put(sc, id);
				idToChan.put(id, sc);

				ExperimentServer<?> exp = tracker.getExperimentForID(id);								
				if( exp != null ) {
					exp.clientConnected(id);
					logger.info("Client reconnected to experiment, " + id.toString(16));
				}
				else {
					logger.severe("Unknown experiment but session is in progress: "
							+ id.toString(16) );
				}
			}
			else if (tracker.sessionCompletedInDB(id) ) {
				/* TODO might want to let user view the graph even if they are already done
				 * In this case, mesh up with sessionIsInProgress above
				 * For now, just close the channel
				 * 
				 * TODO this is now caught way earlier in the login
				 */
				
				logger.info("Client connected to experiment after completion: "
						+ id.toString(16));
				sc.write(encoder.encode(CharBuffer.wrap(Codec.expFinishedAck)));
				sc.close();
			}
			else if( tracker.sessionExistsInDB(id) ) {						
				// Not associated with experiment - send to lobby				
				logger.info(String.format("%s (%s) connected to lobby",
						id.toString(16), tracker.getUsername(id)));
				sc.write(encoder.encode(CharBuffer.wrap(Codec.connectLobbyAck)));
				
				chanToID.put(sc, id);
				idToChan.put(id, sc);
				
				// Record IP address of client for shits and giggles
				tracker.saveIPForSession(id, sc.socket().getInetAddress(), new Date());
			}		
			else {
				logger.warning("Unexpected id tried to connect: " + id.toString(16));
				sc.write(encoder.encode(CharBuffer.wrap(Codec.connectErrorAck)));
				sc.close();
			}

		} catch (CharacterCodingException e) {				
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SessionExpiredException e) {
			logger.warning(id.toString(16) + ": Unexpected connection on expired session!");
			
			try {
				sc.write(encoder.encode(CharBuffer.wrap(Codec.connectErrorAck)));
				sc.close();
			} catch (IOException e1) {				
				e1.printStackTrace();
			}			
		} 
	}

	private void removeUserData(SocketChannel sc) {				
		BigInteger id = chanToID.get(sc);
		logger.warning(id.toString(16) + " disconnected");
		
		// Was this dude in the lobby? If so remove him from the lobby and notify lobby ppl
		if( lobbyStatus.remove(id) != null ) {
			lobbyMessage.remove(id);
			logger.info(String.format("%s (%s) removed from lobby",
					id.toString(16), tracker.getUsername(id)));			
			sendLobbyStatus();
		}				
		
		// Notify the experiment server if this guy was in one (shouldn't overlap with above)
		tracker.sessionDisconnected(id);
		
		// Remove channel-ID mappings (get new one if connect again)
		idToChan.remove(id);
		chanToID.remove(sc);		
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

}
