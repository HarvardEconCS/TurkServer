package edu.harvard.econcs.turkserver.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

public class FakeHITWorkerGroup implements HITWorkerGroup {

	ConcurrentMap<String, FakeHITWorker> idMap = new ConcurrentHashMap<String, FakeHITWorker>();
	
	void addWorker(FakeHITWorker fake) {
		idMap.put(fake.getHitId(), fake);
	}
	
	@Override
	public int groupSize() {		
		return idMap.size();
	}

	@Override
	public Collection<? extends HITWorker> getHITWorkers() {		
		return idMap.values();
	}

	@Override
	public Collection<String> getHITIds() {		
		return idMap.keySet();
	}

	@Override
	public List<String> getWorkerIds() {
		List<String> workerIds = new ArrayList<String>(idMap.size());
		for(FakeHITWorker worker : idMap.values()) workerIds.add(worker.getWorkerId());
		return workerIds;
	}

	@Override
	public HITWorker findByHITId(String hitId) {		
		return idMap.get(hitId);
	}

	public void deliverExperimentBroadcast(Map<String, Object> msg) {
		for( FakeHITWorker fake : idMap.values() ) {
			fake.deliverExperimentBroadcast(msg);
		}		
	}

}
