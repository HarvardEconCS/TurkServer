package edu.harvard.econcs.turkserver.server;

import java.util.Map;

import com.google.inject.Inject;

import edu.harvard.econcs.turkserver.api.BroadcastMessage;
import edu.harvard.econcs.turkserver.api.ExperimentController;
import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.ExperimentServer;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
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
	
	@Inject
	public TestExperiment(
			HITWorkerGroup group,
			ExperimentLog log,
			ExperimentController cont) {
		this.group = group;
		this.log = log;
		this.cont = cont;
	}
	
	@StartExperiment
	void startExp() {
		lastCall = "startExp";
	}
	
	@StartRound
	void startRound(int n) {
		lastCall = "startRound";
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
		return true;
	}
	
	@ServiceMessage
	void service(HITWorker worker, Map<String, Object> msg) {
		lastCall = "service";
	}
}