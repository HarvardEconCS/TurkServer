package edu.harvard.econcs.turkserver.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

public class FakeHITWorkerGroup<C extends FakeHITWorker> implements HITWorkerGroup {

	ConcurrentMap<String, C> idMap = new ConcurrentHashMap<String, C>();
	
	void addWorker(C fake) {
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
	
	public Collection<C> getClassedHITWorkers() {		
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

}
