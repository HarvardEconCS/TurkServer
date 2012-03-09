package edu.harvard.econcs.turkserver;

public class Messages {

	public static final String CONNECT_ERROR = 
		"Unable to connect to the server. The server is either not running,\n" +
		"being restarted, or there is a problem with your internet connection.\n" +
		"Please return this HIT and check back later.";
	
	public static final String UNRECOGNIZED_SESSION = 
		"The server does not recognize this session.\n" +
		"You may be working on a HIT for games that are already completed.\n" +
		"Please return the HIT, and accept a different HIT in this group.\n";

	public static final String SIMULTANEOUS_SESSIONS = 
		"It appears that you have already accepted another HIT for this game.\n" +
		"Please return this HIT and finish your other games before accepting\n" +
		"more HITs. Look for your current game in your dashboard." +
		"Please e-mail us if you believe this message is in error.";

	public static final String EXPIRED_SESSION = 
		"All games for this batch have been completed.\n" +
		"If you have signed up for notifications, we will let you know\n" +
		"when we post more games. Please return the HIT.";

	public static final String TOO_MANY_SESSIONS = 
		"It appears that you have already hit the limit for how many times\n" +
		"you can play in this set of games. If you have asked us to notify you,\n" +
		"we will contact you about future sets of games to participate in.\n" +
		"Please return the HIT.";

	public static final String SESSION_OVERLAP = 
		"This session belongs to someone that has already started a game,\n" +
		"but returned the HIT afterwards, so it can't be reused.\n" +
		"You may return this HIT and take another one from the group.";

	public static final String SESSION_COMPLETED = 
		"This HIT has already been completed by you.\n" +
		"Please submit the HIT below.";

	public static final String SERVER_DISCONNECTED = 
		"The server closed the connection, or your connection was lost.\n" +
		"Please make sure you don't accidentally have two open windows\n" +
		"of the same HIT. The server may also be restarting. Refresh \n" +
		"the page to try and reconnect. Submit the HIT if you haven't already.";

	public static final String QUIZ_FAILED =
		"Sorry, you failed the quiz. You can refresh to try again,\n" +
		"but be careful: if you fail the quiz too many times, you won't\n" +
		"be able to participate in any more games for this session.";

	public static final String TOO_MANY_FAILS =
		"Sorry, you have failed the quiz too many times!\n" +
		"You must wait until our next set of games to participate.\n" +
		"Please return the HIT.";

}
