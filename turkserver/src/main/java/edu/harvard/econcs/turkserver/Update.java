/**
 * A packet of information that the client pulls from the server
 */
package edu.harvard.econcs.turkserver;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Mao
 *
 */
@Deprecated
public abstract class Update implements Serializable {	
	private static final long serialVersionUID = -6290198687173188123L;
	
	public final BigInteger clientID;
	
	protected Update(BigInteger id) { 
		this.clientID = id; 
	}
	
	public String toString() { return this.getClass().getCanonicalName(); }
	
	
	public static abstract class CliUpdate extends Update {
		private static final long serialVersionUID = -3794848343226878755L;
		protected CliUpdate(BigInteger id) { super(id); }
	}
	
	public static abstract class SrvUpdate extends Update {
		private static final long serialVersionUID = -6945954026615999787L;
		protected SrvUpdate(BigInteger id) { super(id); }
	}
	
	public static abstract class UpdateReq extends Update {
		private static final long serialVersionUID = -5942222136839834452L;
		protected UpdateReq(BigInteger id) { super(id); }
	}
	
}

