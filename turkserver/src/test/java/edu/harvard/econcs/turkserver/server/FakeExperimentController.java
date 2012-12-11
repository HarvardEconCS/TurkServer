package edu.harvard.econcs.turkserver.server;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import edu.harvard.econcs.turkserver.api.ExperimentController;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

/**
 * TODO make this more asynchronous in the starting of rounds and experiment
 * @author alicexigao
 *
 */
public class FakeExperimentController implements ExperimentController {

	static final String expId = "fake experiment";
	
	long startTime;
	long finishTime;
	
	HITWorkerGroup group;
	EventAnnotationManager callbacks;
	AtomicInteger roundNum;
	
	public FakeExperimentController(HITWorkerGroup fakeGroup) {
		this.group = fakeGroup;
	}
	
	public void setBean(Object bean) {
		callbacks = new EventAnnotationManager();
		callbacks.processExperiment(expId, bean);
	}
	
	public void startExperiment() {
		startTime = System.currentTimeMillis();
		callbacks.triggerStart(expId);
	}

	public void rcvBroadcast(HITWorker worker, Map<String, Object> msg) {
		callbacks.deliverBroadcastMsg(expId, worker, msg);
	}
	
	public void rcvService(HITWorker worker, Map<String, Object> msg) {
		callbacks.deliverServiceMsg(expId, worker, msg);
	}
	
	@Override
	public String getExpId() {		
		return expId;		
	}

	@Override
	public void sendExperimentBroadcast(Object msg) throws MessageException {
		for( HITWorker fake : group.getHITWorkers() ) {
			((FakeHITWorker) fake).rcvBroadcast(msg);
		}
	}

	@Override
	public void startRounds() {
		roundNum = new AtomicInteger(1);
		callbacks.triggerRound(expId, roundNum.get());
	}

	@Override
	public int getCurrentRound() {		
		return roundNum.get();
	}

	@Override
	public void finishRound() {
		callbacks.triggerRound(expId, roundNum.incrementAndGet());
	}

	@Override
	public void finishExperiment() {
		finishTime = System.currentTimeMillis();
		callbacks.deprocessExperiment(expId);
	}

	@Override
	public long getStartTime() {		
		return startTime;
	}

	@Override
	public long getFinishTime() {		
		return finishTime;
	}

}
