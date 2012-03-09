/**
 * 
 */
package edu.harvard.econcs.turkserver.mturk;

import java.io.IOException;

/**
 * @author mao
 *
 */
public class TurkException extends IOException {

	private static final long serialVersionUID = 9116241223878722076L;

	public TurkException() { super(); }
	public TurkException(String message, Throwable cause) { super(message, cause); }
	public TurkException(String message) { super(message); }
	public TurkException(Throwable cause) {	super(cause); }
	
}
