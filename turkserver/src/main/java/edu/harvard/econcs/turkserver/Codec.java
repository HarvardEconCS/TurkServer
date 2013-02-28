package edu.harvard.econcs.turkserver;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class Codec {
	
	public static final String hitView = "view.hit";
	public static final String hitAccept = "accept.hit";
	public static final String hitSubmit = "submit.hit";
			
	public static final String status_simultaneoussessions = "status.simultaneoussessions";
	public static final String status_sessionoverlap = "status.sessionoverlap";
	public static final String status_toomanysessions = "status.toomanysessions";
	public final static String status_error = "status.error";
		
	// Update messages
	public static final String status_quizneeded = "status.quizrequired";			
	public static final String quizResults = "quiz.results";
	
	public static final String status_quizfail = "status.quizfailed";
	public static final String status_failsauce = "status.toomanyfails";
	
	public static final String status_usernameneeded = "status.usernameneeded";	
	public static final String usernameReply = "username.reply";
	
	public final static String status_connectlobby = "status.connectlobby";	
	public final static String status_connectexp = "status.connectexp";
	
	public static final String startExpError = "experror";
	
	public static final String roundStartMsg = "roundstart";	
	public static final String doneExpMsg = "finishexp";
	
	// Sent when the host server is done with experiments
	public static final String status_expfinished = "status.alreadyfinished";
	
	public static final String status_batchfinished = "status.batchfinished";
	
	
	
	private static final Charset charset = Charset.forName("US-ASCII");
		
	// Channels
	public static final String expChanPrefix = "/experiment/";
	public static final String expSvcPrefix = "/service/experiment/";
	public static CharsetDecoder getDecoder() { return charset.newDecoder(); }
	public static CharsetEncoder getEncoder() { return charset.newEncoder(); }
}
