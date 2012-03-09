package edu.harvard.econcs.turkserver;


/**
 * Used to describe the case where a worker returns a HIT after 
 * an experiment has started, and someone else tries to take the HIT
 * @author mao
 *
 */
public class SessionOverlapException extends ExpServerException {
	
	private static final long serialVersionUID = -1053171108174665098L;

}
