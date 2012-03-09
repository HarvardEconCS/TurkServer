package edu.harvard.econcs.turkserver;

public class ExpServerException extends Exception {
	
	private static final long serialVersionUID = -2572797673060795529L;

	public ExpServerException() {}

	public ExpServerException(String message) {	super(message); }

	public ExpServerException(Throwable cause) { super(cause); }

	public ExpServerException(String message, Throwable cause) { super(message, cause); }

}
