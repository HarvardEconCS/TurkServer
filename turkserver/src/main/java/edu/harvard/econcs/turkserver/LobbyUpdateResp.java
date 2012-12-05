package edu.harvard.econcs.turkserver;

import edu.harvard.econcs.turkserver.Update.SrvUpdate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class LobbyUpdateResp extends SrvUpdate {
	private static final long serialVersionUID = 6705096860630116385L;
		
	public final List<UserStatus> userList;
	
	public final int usersNeeded;
	public final boolean joinEnabled;
	
	public final String serverMessage;
	public final int gamesInProgress;
	public final int totalConnections;
	
	public LobbyUpdateResp(
			BigInteger id, int usersNeeded, 
			boolean joinEnabled, int listSize,
			String serverMessage, int gamesInProgress, int totalConnections) { 
		super(id);		
		this.usersNeeded = usersNeeded;
		this.joinEnabled = joinEnabled;
		
		this.serverMessage = serverMessage;
		this.gamesInProgress = gamesInProgress;
		this.totalConnections = totalConnections;
		
		userList = new ArrayList<UserStatus>(listSize);
	}

	public void addRecord(BigInteger id, String username, boolean status, String message) {
		userList.add(new UserStatus(id, username, status, message));
	}
}
