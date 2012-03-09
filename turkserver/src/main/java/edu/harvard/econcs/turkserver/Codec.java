package edu.harvard.econcs.turkserver;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class Codec {

	// Connection ACK messages
	public final static String connectLobbyAck = "Lobby";
	public final static String reconnectExpAck = "Experiment";
	public static final String expFinishedAck = "AlreadyFinished";	
	public final static String connectErrorAck = "Error";
	
	// Update messages
	public static final String lobbyUpdateMsg = "LobbyUpdate";
	// Start exp message happens when the ExpServer sends its own notification
	public static final String startExpError = "ExpStartError";
	public static final String doneExpMsg = "FinishedExp";
	// Sent when the host server is done with experiments
	public static final String batchFinishedMsg = "BatchFinished";
	
	private static final Charset charset = Charset.forName("US-ASCII");
	
	public static enum LoginStatus {
		ERROR,
		QUIZ_REQUIRED,
		NEW_USER,
		REGISTERED
	}
	
    public static CharsetDecoder getDecoder() { return charset.newDecoder(); }
	public static CharsetEncoder getEncoder() { return charset.newEncoder(); }
}
