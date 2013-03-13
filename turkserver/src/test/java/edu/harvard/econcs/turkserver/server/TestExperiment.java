package edu.harvard.econcs.turkserver.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import edu.harvard.econcs.turkserver.api.BroadcastMessage;
import edu.harvard.econcs.turkserver.api.ExperimentController;
import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.ExperimentServer;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.api.IntervalEvent;
import edu.harvard.econcs.turkserver.api.ServiceMessage;
import edu.harvard.econcs.turkserver.api.StartExperiment;
import edu.harvard.econcs.turkserver.api.StartRound;
import edu.harvard.econcs.turkserver.api.TimeLimit;
import edu.harvard.econcs.turkserver.api.WorkerConnect;
import edu.harvard.econcs.turkserver.api.WorkerDisconnect;

@ExperimentServer 
class TestExperiment {
	volatile String lastCall = null;
	
	HITWorkerGroup group;
	ExperimentLog log;
	ExperimentController cont;
	
	int setSize;
	int rounds;
	Set<String> uniqueMessages;
	
	int intervalCalls = 0;
	
	@Inject
	public TestExperiment(
			HITWorkerGroup group,
			ExperimentLog log,
			ExperimentController cont) {
		this.group = group;
		this.log = log;
		this.cont = cont;
		
		uniqueMessages = Collections.synchronizedSet(new HashSet<String>());
	}
	
	void init(int groupSize, int rounds) {
		setSize = groupSize;
		this.rounds = rounds;
	}
	
	@StartExperiment
	void startExp() {
		lastCall = "startExp";
		
		if( cont != null ) {
			if( rounds > 0 ) cont.startRounds();
			else {
				try {
					cont.sendExperimentBroadcast(
							ImmutableMap.<String, Object>of("status", "something start")
							);
				} catch (MessageException e) { e.printStackTrace(); }
			}
		}
	}
	
	@StartRound
	void startRound(int n) {
		lastCall = "startRound";
	}
	
	@IntervalEvent
	void interval() {
		intervalCalls++;
		lastCall = "intervalEvent";
	}
	
	@TimeLimit
	void timeLimit() {
		lastCall = "timeLimit";
	}
	
	@WorkerConnect
	void connect(HITWorker worker) {
		lastCall = "connect";
	}
	
	@WorkerDisconnect
	void disconnect(HITWorker worker) {
		lastCall = "disconnect";
	}
	
	@BroadcastMessage
	boolean broadcast(HITWorker worker, Map<String, Object> msg) {
		lastCall = "broadcast";
		if( msg == null ) return true;
		
		Object message = msg.get("msg");
		if( message != null ) uniqueMessages.add(message.toString());
		checkStatus();
		return true;
	}
	
	@ServiceMessage
	void service(HITWorker worker, Map<String, Object> msg) {
		lastCall = "service";
		if( msg == null ) return;
		
		Object message = msg.get("msg");
		if( message != null ) uniqueMessages.add(message.toString());
		checkStatus();
	}

	private synchronized void checkStatus() {
		if( uniqueMessages.size() < setSize ) return;			
		
		uniqueMessages.clear();
		
		if( cont.getCurrentRound() < rounds ) {
			log.print("Finishing round");
			cont.finishRound();
		}			
		else {
			log.print("Finishing experiment");
			cont.finishExperiment();
		}
	}
}