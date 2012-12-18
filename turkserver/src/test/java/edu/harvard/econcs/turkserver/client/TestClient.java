package edu.harvard.econcs.turkserver.client;

import java.util.Map;
import java.util.Random;

import com.google.common.collect.ImmutableMap;

import edu.harvard.econcs.turkserver.api.BroadcastMessage;
import edu.harvard.econcs.turkserver.api.ClientController;
import edu.harvard.econcs.turkserver.api.ClientError;
import edu.harvard.econcs.turkserver.api.ExperimentClient;
import edu.harvard.econcs.turkserver.api.FinishExperiment;
import edu.harvard.econcs.turkserver.api.ServiceMessage;
import edu.harvard.econcs.turkserver.api.StartExperiment;
import edu.harvard.econcs.turkserver.api.StartRound;
import edu.harvard.econcs.turkserver.api.TimeLimit;

@ExperimentClient
public class TestClient {
	
	public volatile String lastCall = null;		
	ClientController cont;
	
	volatile String message;
	volatile int delay;
	
	Random rnd = new Random();
	
	public TestClient(ClientController cont) {
		this.cont = cont;			
	}
	
	public void setMessage(String message, int delay) {
		this.message = message;
		this.delay = delay;
	}
	
	@StartExperiment
	void startExp() {
		lastCall = "startExp";
	}
	
	@StartRound
	void startRound(int n) {
		lastCall = "startRound";
		
		new Thread() {
			public void run() {
				try { Thread.sleep(rnd.nextInt(delay));	}
				catch (InterruptedException e) {}
				cont.sendExperimentBroadcast(ImmutableMap.of("msg", (Object) message));
			}
		}.start();
	}	
	
	@TimeLimit
	void timeLimit() {
		lastCall = "timeLimit";
	}
	
	@FinishExperiment
	void finishExp() {
		lastCall = "finishExp";
	}
	
	@ClientError
	void clientError(String err) {
		lastCall = "clientError";
	}
	
	@BroadcastMessage
	void broadcast(Map<String, Object> msg) {
		System.out.println("Got broadcast: " + msg.toString());
		lastCall = "broadcast";			
	}
	
	@ServiceMessage
	void service(Map<String, Object> msg) {
		System.out.println("Got service: " + msg.toString());
		lastCall = "service";
	}
}