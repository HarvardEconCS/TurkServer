/**
 * 
 */
package edu.harvard.econcs.turkserver;

import edu.harvard.econcs.turkserver.Codec.LoginStatus;
import edu.harvard.econcs.turkserver.Update.*;

import java.math.BigInteger;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * @author Mao
 *
 */
public interface Updater extends Remote {
	
	/**
	 * Sent before the connect via RMI to update the server when a HIT is accepted
	 * @param sessionID
	 * @param assignmentId
	 * @param workerId
	 * @param hitId
	 * @param username
	 * @return true if this user needs a username for the lobby (first time connecting)
	 * @throws RemoteException
	 */
	public LoginStatus sessionLogin(BigInteger sessionID, String assignmentId, String workerId) throws RemoteException;
	
	/**
	 * Gets the data for a quiz to be assigned to the worker
	 * @param sessionID
	 * @param assignmentId
	 * @param workerId
	 * @return
	 */
	public QuizMaterials getQuizMaterials(BigInteger sessionID,	String assignmentId, String workerId) throws RemoteException;

	/**
	 * Sends the results of a quiz back to the server
	 * @param qr 
	 * @throws RemoteException 
	 */
	public void sendQuizResults(
			BigInteger sessionID, String assignmentId,
			String workerId, QuizResults qr) 
	throws RemoteException;

	/**
	 * Send a username before we connect.
	 * @param sessionID
	 * @param username
	 * @return
	 * @throws RemoteException
	 */
	public boolean lobbyLogin(BigInteger sessionID, String username) throws RemoteException;
	
	/**
	 * Method called by client threads to send a state change
	 * 
	 * @param u
	 * @return
	 * @throws RemoteException
	 */
	public boolean clientUpdate(CliUpdate u) throws RemoteException;

	/**
	 * Method called by client threads to get information
	 * 
	 * @param clientID
	 * @return
	 * @throws RemoteException
	 */
	public SrvUpdate pullUpdate(UpdateReq clientID) throws RemoteException;
	
}
