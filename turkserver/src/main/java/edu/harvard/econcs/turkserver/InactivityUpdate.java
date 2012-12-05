package edu.harvard.econcs.turkserver;

import edu.harvard.econcs.turkserver.Update.CliUpdate;

import java.math.BigInteger;

@Deprecated
public class InactivityUpdate extends CliUpdate {
	private static final long serialVersionUID = -8457361601929610016L;
	
	public final long timeInactive;
	
	public InactivityUpdate(BigInteger id, long timeInactive) {
		super(id);
		
		this.timeInactive = timeInactive;
	}

}
