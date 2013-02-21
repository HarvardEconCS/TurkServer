package edu.harvard.econcs.turkserver;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class Codec {
	
	public static final String hitView = "view.hit";
	public static final String hitAccept = "accept.hit";
	public static final String hitSubmit = "submit.hit";
	
	public static final String expFinishedAck = "alreadyfinished";	
	public final static String connectErrorAck = "error";
		
	// Update messages
	public static final String quizNeeded = "quiz.required";			
	public static final String quizResults = "quiz.results";
	
	public static final String quizFail = "quiz.failed";	
	
	public static final String usernameNeeded = "username";	
	public static final String usernameReply = "username.reply";
	
	public final static String connectLobbyAck = "lobby";	
	public final static String connectExpAck = "startexp";
	
	public static final String startExpError = "experror";
	
	public static final String roundStartMsg = "roundstart";	
	public static final String doneExpMsg = "finishexp";
	// Sent when the host server is done with experiments
	public static final String batchFinishedMsg = "batchfinished";
	
	private static final Charset charset = Charset.forName("US-ASCII");
		
	// Channels
	public static final String expChanPrefix = "/experiment/";
	public static final String expSvcPrefix = "/service/experiment/";	

	public static CharsetDecoder getDecoder() { return charset.newDecoder(); }
	public static CharsetEncoder getEncoder() { return charset.newEncoder(); }
}
