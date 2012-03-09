package edu.harvard.econcs.turkserver.client;

import edu.harvard.econcs.turkserver.*;
import edu.harvard.econcs.turkserver.Codec.LoginStatus;
import edu.harvard.econcs.turkserver.Update.*;
import edu.harvard.econcs.turkserver.server.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.CharsetDecoder;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public abstract class NetExpClient implements AbstractExpClient {
	private final ByteBuffer buf = ByteBuffer.allocate(HostServer.BUF_LEN);
	private final CharsetDecoder decoder = Codec.getDecoder();

	protected final Logger logger;

	protected GUIController<? extends AbstractExpClient> gc;
	protected State state;

	public final BigInteger sessionID;
	public final String assignmentId;
	public final String workerId;

	private final String server;
	private final int serverPort;	

	private SocketChannel sc;
	private Selector sel;		
	
	private ConcurrentLinkedQueue<Update> pendingSends;	
	private Updater stub;

	private volatile boolean isClosed = false;
	
	boolean isReady = false;	

	public NetExpClient(BigInteger sessionID, String server, int serverPort,
			String assignmentId, String workerId) {
		this.sessionID = sessionID;
		this.server = server;
		this.serverPort = serverPort;
		this.assignmentId = assignmentId;
		this.workerId = workerId;

		state = State.DISCONNECTED;
		isReady = false;

		pendingSends = new ConcurrentLinkedQueue<Update>();

		logger = Logger.getLogger(this.getClass().getSimpleName() + sessionID.toString());		
	}

	public void setGC(GUIController<? extends AbstractExpClient> gc) { this.gc = gc; }

	public BigInteger getSessionID() { return sessionID; }

	@Override
	public void updateLobbyReadiness(boolean isReady) {
		if( this.isReady != isReady ) {
			this.isReady = isReady;
			asyncUpdate(new LobbyStatusUpdate(sessionID, isReady));
		}
	}		
	
	@Override
	public void updateLobbyStatus(String statusMsg) {		
		asyncUpdate(new LobbyStatusUpdate(sessionID, isReady, statusMsg));
	}

	@Override
	public void recordInactivity(long timeInactive) {
		asyncUpdate(new InactivityUpdate(sessionID, timeInactive));		
	}
	
	/**
	 * Asynchronously send an update to the server
	 * or call the stub's update request method
	 * to minimize GUI lag in swing
	 * @param u
	 */	
	public void asyncUpdate(Update u) {
		pendingSends.offer(u);
		sel.wakeup();
	}		

	private void serverUpdate(SrvUpdate u) {
		if( u instanceof LobbyUpdateResp ) {
			if( state == State.EXPERIMENT ) {
				/* TODO Currently ignoring
				 * fix this in the future, but commented for now to eliminate a bug source
				 */
				return;
				
//				gc.setStatus(StatusBar.returningToLobbyMsg);
//				gc.lobbyRedraw();
//				state = State.LOBBY;
			}

			// Process updated lobby thing
			LobbyUpdateResp lup = (LobbyUpdateResp) u;
			gc.getLobby().updateModel(lup);
			gc.setStatus(lup.joinEnabled 
					? StatusBar.lobbyReadyMsg 
							: StatusBar.lobbyWaitingMsg );
		} else {
			// Some sort of other update - enter experiment!
			if( state == State.LOBBY ) {
				state = State.EXPERIMENT;
				gc.setStatus(StatusBar.enteringExpMsg);
				gc.experimentRedraw();				
			}

			expServerUpdate(u);
		}
	}

	private UpdateReq updateReq(String msg) {
		if (Codec.lobbyUpdateMsg.equals(msg)) {
			// Can't use our state to determine this, since we might be entering experiment
			return new LobbyUpdateReq(sessionID);
		} else if( Codec.startExpError.equals(msg)) {
			// Server unilaterally disabled due to error, disable join button
			isReady = false;
			gc.getLobby().setJoinEnabled(false);
			gc.popMsg("Unable to start experiment!" +
					"There might be a problem with the server." +
					"You may try again, or just return the HIT.",
					"Internal Server Error", JOptionPane.ERROR_MESSAGE);
			return new LobbyUpdateReq(sessionID);
		} else if( Codec.doneExpMsg.equals(msg)) {
			gc.setStatus(StatusBar.finishedExpMsg);
			// Do nothing
			return null;
		} else if( Codec.batchFinishedMsg.equals(msg)) {
			gc.blankRedraw("All games finished");
			gc.setStatus(StatusBar.batchFinishedMsg);
			gc.popMsg("All games for this batch have been completed.\n" +
					"If you have signed up for notifications, we will let you know\n" +
					"when we post more games. Please return the HIT.",
					"Game Batch Completed", JOptionPane.WARNING_MESSAGE);
			close();
			return null;
		} else {
			return expUpdateReq(msg);
		}
	}

	/**
	 * Process updates from the server
	 * @param u
	 */		
	protected abstract void expServerUpdate(SrvUpdate u);

	/**
	 * Client figures out update request based on its state
	 * @return
	 */	
	protected abstract UpdateReq expUpdateReq(String msg);

	protected abstract void enableSubmit();

	public void run() {
		Thread.currentThread().setName(this.getClass().getSimpleName() 
				+ " session " + sessionID.toString(16));
		logger.info("Begin client log with session ID " + sessionID.toString(16));
		
		// Set up RMI
		try {
			gc.setStatus("Looking up server...");
			logger.info("Getting RMI registry on " + server);
			Registry reg = LocateRegistry.getRegistry(server);
			stub = (Updater) reg.lookup(Updater.class.getSimpleName());
		} catch (Exception e) {
			e.printStackTrace();
			gc.blankRedraw("Unable to connect");
			gc.setStatus("Unable to connect to server");
			gc.popMsg(Messages.CONNECT_ERROR, "Connection Error", JOptionPane.ERROR_MESSAGE);
			return;
		}		
		
		try {			
			LoginStatus ls;
			if( assignmentId != null ) {
				/* Try logging in if we are not testing locally
				 * might have to do a quiz and then a user name
				 */
				do {				
					ls = stub.sessionLogin(sessionID, assignmentId, workerId);
					// Do we need to do a quiz?
					if( ls == LoginStatus.QUIZ_REQUIRED ) {
						QuizMaterials qm = stub.getQuizMaterials(sessionID, assignmentId, workerId);
						if( qm == null ) {
							logger.severe("Required to take quiz, but got null quiz");
							break;
						}
						QuizResults qr = gc.doQuiz(qm);
						stub.sendQuizResults(sessionID, assignmentId, workerId, qr);
					}
					else if( ls == LoginStatus.NEW_USER ) {
						/*
						 * Ask user for username if we are running on Turk
						 * and this assignment hasn't already been registered to this session
						 */				 
						gc.setStatus("Waiting for nickname...");

						String username = null;
						do {
							username = gc.questionMsg("Enter a nickname for yourself:");
						} while ( "".equals(username) );

						gc.setStatus("Sending nickname to server...");
						stub.lobbyLogin(sessionID, username);
						
						break; // Save the extra RMI call
					}
				} while( ls != LoginStatus.REGISTERED );
				
			}	
		} catch( RemoteException e ) {
			// Get the cause of the server-side Remote Exception
			Throwable t = e.getCause().getCause();		
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
			return;
		} 

		// Connect to Server
		connectToServer();
		
		// Draw the lobby
		gc.lobbyRedraw();

		// Switch into update waiting mode
		while( !isClosed ) {
			Iterator<SelectionKey> it = null;
			try {			
				if( !pendingSends.isEmpty() || sel.select() == 0) {
					// quit the thread if close() was called
					if( isClosed ) break;

					while ( !pendingSends.isEmpty() ) {
						Update u = null;
							
						// Concat adjacent updates that are equal
						do {
							u = pendingSends.poll();
						} while( !pendingSends.isEmpty() && u.equals(pendingSends.peek()) );						
						
						if( u instanceof UpdateReq ) {
							serverUpdate(stub.pullUpdate((UpdateReq) u));
						}		
						else if ( u instanceof CliUpdate ) {
							stub.clientUpdate((CliUpdate) u);
						}
						else {
							logger.warning("Unrecognized client update in queue: " + u.toString());
						}
					}

					// Go back to the select since we got here by queued messages
					continue; 
				}

				it = sel.selectedKeys().iterator();

				while(it.hasNext()) {
					SelectionKey sk = it.next();
					it.remove();
					if( sk.isReadable() ) {
						buf.clear();
						logger.fine("Entering read");
						
						int bytes = sc.read(buf); 
						if( bytes == -1 ) {
							// Stream closed
							gc.blankRedraw("Server disconnected");
							gc.setStatus("Server disconnected");
							gc.popMsg(Messages.SERVER_DISCONNECTED,	"Server Disconnected", JOptionPane.WARNING_MESSAGE);
							state = State.DISCONNECTED;
							close();							
							break;
						}

						buf.flip();

						String[] msgs = decoder.decode(buf).toString().split("\\s+");						
						logger.info("Received mesg(s): " + Arrays.toString(msgs));

						// If we got multiple messages in one, need to concatenate them one at a time
						for( String msg : msgs ) {						
							/* TODO properly distinguish between lobby/different things
							 * instead of using this silly updateReq
							 * 
							 */
							UpdateReq ur = updateReq(msg);
							// Send asynchronously
							if( ur != null ) asyncUpdate(ur);							
						}												
					}
				}

			} catch(CancelledKeyException e) {
				/* TODO from sk.isReadable(), indicates applet closed?
				 * but we replaced the close to work properly, so
				 * maybe reconnect? 
				 */
				logger.info("Connection destroyed, closing");
				break;
			} catch(ClosedSelectorException e) {
				logger.info("Selector closed, breaking out of main loop");
				break;
			} catch(ClosedChannelException e) { // Subclass of IOException
				// SocketChannel is closed. Probably from server?
				e.printStackTrace();
				break;
			} catch(RemoteException e) { // These two guys shouldn't break...
				e.printStackTrace();
				// TODO should we pay a bonus for this one too?
				gc.popMsg("Error: " + e.toString() + ".\n" +
						"Keep playing if possible, but email your Java " +
						"console log to us for a possible extra bonus.");				
			} catch (IOException e) {
				e.printStackTrace();
				gc.popMsg("Error: " + e.toString() + ".\n" +
						"If your game no longer works, you may have lost connection. " +
						"If this is the case, go to your assigned HITs to reload.");
			} catch (RuntimeException e) {
				/* A last ditch effort to stop stupid shit, and find out what is going on
				 * TODO send this to the server automatically
				 */
				e.printStackTrace();				
				Throwable t = e.getCause();
				if( t != null ) {
					t.printStackTrace();					
				}
				gc.popMsg("Bingo! You almost crashed and froze, but we caught the error \n" +
						"and recorded it in your Java console log. Finish the game and \n" +
						"before you submit, paste the log in the comment box or email it \n" +
						"to the requester (even better) to get a nice extra bonus.",
						"You just won!", JOptionPane.WARNING_MESSAGE);
			}
		}		
		
		// Close again just in case it hasn't been called
		close();
		logger.info("Client thread finished");
	}

	private void connectToServer() {
		while( !isClosed ) {
			try {
				logger.info(String.format("Opening connection to %s:%d", server, serverPort));
				gc.setStatus("Trying server connection...");
				sc = SocketChannel.open(new InetSocketAddress(server, serverPort));

				// Send session ID to server
				buf.clear();
				buf.put(sessionID.toByteArray());
				buf.flip();			
				int bytesWritten = sc.write(buf);
				logger.fine(bytesWritten + " bytes Written");			
				logger.info("Sent session ID to server:" + sessionID.toString(16));

				// Read response from server
				buf.clear();
				sc.read(buf);
				buf.flip();

				CharBuffer cb = decoder.decode(buf);
				String resp = cb.toString();
				logger.fine("Connect response: " + resp);

				if( Codec.connectLobbyAck.compareTo(resp) == 0) {
					state = State.LOBBY;
					gc.setStatus("Connected to lobby");
					logger.info("Client connected to lobby");

					// Queue initial status to server
					pendingSends.offer(new LobbyStatusUpdate(sessionID, isReady));
				} 
				else if (Codec.reconnectExpAck.compareTo(resp) == 0) {
					/* TODO this is not explicitly lobby but it will get flipped when the server updates
					 * TODO ensure that this is working properly
					 */
					state = State.LOBBY;
					
					gc.setStatus("Connecting to previous game");
					logger.info("Client is reconnecting to an experiment");
					
					// pull a full update
					pendingSends.offer(expUpdateReq(null));
				} 
				else if( Codec.expFinishedAck.compareTo(resp) == 0) {
					// Connected to exp that is already finished. Enable the submit.
					// TODO fix this if want to allow view graph for finished exp
					gc.blankRedraw("Experiment already finished");
					gc.setStatus("Experiment already finished.");
					logger.info("Connected to experiment that is already done");
					gc.popMsg("This experiment is already finished.\n" +
							"Please submit the HIT below.");
					enableSubmit();
					close();
					break;
				}
				else if ( Codec.connectErrorAck.compareTo(resp) == 0 ) {
					gc.blankRedraw("Connection Error");
					gc.setStatus("Connection Error.");
					logger.severe("Server refused connection");
					gc.popMsg("There was an error connecting to the server.\n" +
							"Please refresh the page to try again, or return the HIT.");					
					close();
					break;
				}
				else {
					gc.setStatus("Unable to connect, retrying in a few seconds...");
					logger.severe("Connection error");
					Thread.sleep(2000);
					continue;
				}				

				sel = SelectorProvider.provider().openSelector();
				sc.configureBlocking(false);

				sc.register(sel, SelectionKey.OP_READ);
				
				// connected successfully
				break;				
			} catch(ConnectException e) {
				// open connection failed - this only happens if RMI is up but socket is not
				e.printStackTrace();
				gc.setStatus("Connection error");
				gc.popMsg("Error connecting to the server. Please check your internet connection,\n" +
						"or the server may be down. You may return the HIT.",
						"Error connecting to server", JOptionPane.ERROR_MESSAGE);
				close();
				break;				
			} catch (InterruptedException e) {				
				e.printStackTrace();
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}
	}

	public void close() {
		isClosed = true;
		
		// TODO free up RMI resources 
		
		try {
			if( sc != null) sc.close();
			if( sel != null) sel.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}		
	}
	
}


