package edu.harvard.econcs.turkserver;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Comparator;

@Deprecated
public class UserStatus implements Serializable {

	private static final long serialVersionUID = 2305358626684670328L;

	public final BigInteger sessionID;
	public final String userName;
	public final boolean isReadyLobby;
	public final String message;
	
	public UserStatus(BigInteger sessionID, String userName, boolean isReady, String message) {
		this.sessionID = sessionID;
		this.userName = userName;
		this.isReadyLobby = isReady;
		this.message = message;
	}
		
	/**
	 * Sorts by username and if that matches, then session ID
	 * @author mao
	 *
	 */
	public static class UsernameComparator implements Comparator<UserStatus> {
		@Override
		public int compare(UserStatus o1, UserStatus o2) {			
			if( o1.userName != null ) {
				int comp = o1.userName.compareTo(o2.userName);
				if( comp != 0 ) return comp;				
			}
			
			return o1.sessionID.compareTo(o2.sessionID);
		}		
	}
}
