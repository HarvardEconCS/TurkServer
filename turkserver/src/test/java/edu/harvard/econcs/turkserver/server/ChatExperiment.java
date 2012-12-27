package edu.harvard.econcs.turkserver.server;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
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
	HITWorkerGroup group;
	ExperimentLog log;
	ExperimentController controller;
	
	@Inject
	public ChatExperiment(
			HITWorkerGroup group,
			ExperimentLog log,
			ExperimentController controller) {
		this.group = group;
		this.log = log;
		this.controller = controller;
	}
	
	@StartExperiment
	void start() throws MessageException {
		log.printf("Starting chat with %d people", group.groupSize());
		Map<String, Object> data = ImmutableMap.of(
						"msg", (Object) "Please start chatting!");
		controller.sendExperimentBroadcast(data);		
	}
	
	@BroadcastMessage
	boolean chatMessage(HITWorker worker, Map<String, Object> data) {
		log.printf("Worker %s said: %s", worker.getUsername(), data.get("msg"));		
		return true;
	}
	
	@TimeLimit
	void timedOut() throws MessageException {
		Map<String, Object> data = ImmutableMap.of(
				"msg", (Object) "No more chatting for you!");
		controller.sendExperimentBroadcast(data);		
	}
}
