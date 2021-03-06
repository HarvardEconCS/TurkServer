package edu.harvard.econcs.turkserver.server;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import edu.harvard.econcs.turkserver.api.ExperimentController;
import edu.harvard.econcs.turkserver.api.HITWorker;

/**
 * TODO make this more asynchronous in the starting of rounds and experiment
 * @author alicexigao
 *
 */
public class FakeExperimentController implements ExperimentController {

	static final String expId = "fake experiment";
	
	long startTime;
	long finishTime;
	
	FakeHITWorkerGroup group;
	EventAnnotationManager callbacks;
	AtomicInteger roundNum;
	
	public FakeExperimentController(FakeHITWorkerGroup fakeGroup) {
		this.group = fakeGroup;
		
		callbacks = new EventAnnotationManager();
		
		for(HITWorker fake : fakeGroup.getHITWorkers() )
			((FakeHITWorker) fake).initialize(expId, callbacks);
	}
	
	public void addFakeGroup(FakeHITWorkerGroup fakeGroup) {
		// TODO This doesn't actually track this group, so fix it		
		for(HITWorker fake : fakeGroup.getHITWorkers() )
			((FakeHITWorker) fake).initialize(expId, callbacks);
	}

	public void setBean(Object bean) {		
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
	public void sendExperimentBroadcast(Map<String, Object> msg) {
		group.deliverExperimentBroadcast(msg);		
	}

	@Override
	public void setRoundInput(String inputData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBonusAmount(HITWorker hitWorker, double amount) {
		// TODO Auto-generated method stub		
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
