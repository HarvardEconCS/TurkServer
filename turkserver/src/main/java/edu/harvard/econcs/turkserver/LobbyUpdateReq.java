package edu.harvard.econcs.turkserver;

import edu.harvard.econcs.turkserver.Update.UpdateReq;

import java.math.BigInteger;

/**
 * 
 * @author mao
 *
 */
public class LobbyUpdateReq extends UpdateReq {
	private static final long serialVersionUID = -7376155581194101028L;
	
	public LobbyUpdateReq(BigInteger id) { 
		super(id);
	}
	
	@Override
	public boolean equals(Object obj) {		
		if( obj instanceof LobbyUpdateReq )
			return true;
		else
			return false;
	}
	
	
}