package edu.harvard.econcs.turkserver;

import java.math.BigInteger;

import edu.harvard.econcs.turkserver.Update.CliUpdate;

@Deprecated
public class LobbyStatusUpdate extends CliUpdate {	

	private static final long serialVersionUID = -1975999707703711064L;
	
	public final boolean isReady;
	public final String msg;
	
	public LobbyStatusUpdate(BigInteger id, boolean isReady) {
		super(id);
		this.isReady = isReady;
		msg = null;
	}

	public LobbyStatusUpdate(BigInteger sessionID, boolean isReady, String statusMsg) {
		super(sessionID);
		this.isReady = isReady;
		this.msg = statusMsg;
	}

}
