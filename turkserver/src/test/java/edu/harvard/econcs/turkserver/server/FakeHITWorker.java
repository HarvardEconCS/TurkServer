package edu.harvard.econcs.turkserver.server;

import java.net.InetAddress;
import java.util.Map;

import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.api.ClientController;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.client.ClientAnnotationManager;
import edu.harvard.econcs.turkserver.server.MessageException;

/**
 * A testing class that wires together server and client side communication without cometD
 * 
 * @author mao
 *
 */
public final class FakeHITWorker implements HITWorker, ClientController {

	protected String hitId;
	protected String assignmentId;
	protected String workerId;
	protected String username;	
	
	ClientAnnotationManager<?> clientWrapper;
	
	// This is set AFTER instantiation
	FakeExperimentController expCont;
	
	public static <C> FakeHITWorker getNew(String hitId, String assignmentId, String workerId, String username,
			Class<C> clientClass) throws Exception {
		FakeHITWorker fakeWorker = new FakeHITWorker();		
		fakeWorker.hitId = hitId;
		fakeWorker.assignmentId = assignmentId;
		fakeWorker.workerId = workerId;
		fakeWorker.username = username;
					
		fakeWorker.clientWrapper = new ClientAnnotationManager<C>(fakeWorker, clientClass);
		return fakeWorker;
	}	
	
	public Object getClientBean() {
		return clientWrapper.getClientBean();
	}
	
	@Override
	public String getHitId() {		
		return hitId;
	}

	@Override
	public String getAssignmentId() {		
		return assignmentId;
	}

	@Override
	public String getWorkerId() {		
		return workerId;
	}

	@Override
	public String getUsername() {		
		return username;
	}

	@Override
	public InetAddress getIPAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() {		
		return true;
	}

	@Override
	public long getDisconnectedTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getLiveNumDisconnects() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getLiveInactivePercent() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLiveInactiveDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	void deliverExperimentBroadcast(Map<String, Object> msg) {
		clientWrapper.deliverBroadcast(msg);		
	}

	@Override
	public void deliverExperimentService(Map<String, Object> msg) throws MessageException {
		clientWrapper.deliverService(msg);		
	}

	@Override
	public String toString() {
		return "Fake HITWorker with " + hitId;
	}

	/* ****************************************
	 * Client-side methods
	 ******************************************/
	
	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendQuizResults(QuizResults qr) {
		// TODO Auto-generated method stub
		System.out.println("Sending quiz results not implemented yet");
	}

	@Override
	public void sendExperimentBroadcast(Map<String, Object> data) {
		expCont.rcvBroadcast(this, data);
	}

	@Override
	public void sendExperimentService(Map<String, Object> data) {
		expCont.rcvService(this, data);
	}

	@Override
	public void recordInactivity(long timeInactive) {
		// TODO Auto-generated method stub
		System.out.println("Recording inactivity not implemented yet");
	}
}
