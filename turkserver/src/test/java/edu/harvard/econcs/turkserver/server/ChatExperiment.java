package edu.harvard.econcs.turkserver.server;

import java.util.Map;

import com.google.inject.Inject;

import edu.harvard.econcs.turkserver.api.*;

/**
 * Sample experiment demonstrating the API.
 * (For screenshots)
 * @author mao
 *
 */

@ExperimentServer("chat")
public class ChatExperiment {
	@Inject HITWorkerGroup group;
	@Inject ExperimentLog log;
	@Inject ExperimentController controller;
	
	@StartExperiment
	void start() throws MessageException {
		log.printf("Starting chat with %d people", group.groupSize());
		controller.sendExperimentBroadcast("Please start chatting!");		
	}
	
	@BroadcastMessage
	boolean chatMessage(HITWorker worker, Map<String, Object> data) {
		log.printf("Worker %s said: %s", worker.getUsername(), data.get("msg"));		
		return true;
	}
	
	@TimeLimit
	void timedOut() throws MessageException {
		controller.sendExperimentBroadcast("No more chatting for you!");		
	}
}
