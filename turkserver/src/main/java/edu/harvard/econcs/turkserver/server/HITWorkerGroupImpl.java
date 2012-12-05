package edu.harvard.econcs.turkserver.server;

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
	
	@Override
	public int groupSize() {		
		return workerSet.size();
	}

	@Override
	public Collection<? extends HITWorker> getHITWorkers() {		
		return workerSet.values();
	}

	@Override
	public List<String> getHITIds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getWorkerIds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HITWorker findByHITId(String hitId) {		
		return workerSet.get(hitId);
	}

}
