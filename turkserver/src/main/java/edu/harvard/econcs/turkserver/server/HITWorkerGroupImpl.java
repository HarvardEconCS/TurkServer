package edu.harvard.econcs.turkserver.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

public class HITWorkerGroupImpl implements HITWorkerGroup {

	private Map<String, HITWorkerImpl> workerSet = new HashMap<String, HITWorkerImpl>();
	
	void add(HITWorkerImpl worker) {
		workerSet.put(worker.getHitId(), worker);
	}
	
	public void setExperiment(ExperimentControllerImpl cont) {
		for( HITWorkerImpl hitw : workerSet.values() )
			hitw.setExperiment(cont);		
	}

	@Override
	public int groupSize() {		
		return workerSet.size();
	}

	@Override
	public boolean contains(HITWorker hitWorker) {		
		String hitId = hitWorker.getHitId();
		return workerSet.containsKey(hitId) && workerSet.get(hitId).equals(hitWorker);
	}

	@Override
	public Collection<? extends HITWorker> getHITWorkers() {		
		return workerSet.values();
	}

	@Override
	public List<String> getHITIds() {
		List<String> hitIds = new ArrayList<String>(workerSet.size());
		for( HITWorkerImpl hitw : workerSet.values() ) hitIds.add(hitw.getHitId());
		return hitIds;
	}

	@Override
	public List<String> getWorkerIds() {
		List<String> workerIds = new ArrayList<String>(workerSet.size());
		for( HITWorkerImpl hitw : workerSet.values() ) workerIds.add(hitw.getWorkerId());
		return workerIds;
	}

	@Override
	public HITWorker findByHITId(String hitId) {		
		return workerSet.get(hitId);
	}

}
