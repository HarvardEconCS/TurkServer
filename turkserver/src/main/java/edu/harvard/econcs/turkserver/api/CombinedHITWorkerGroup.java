package edu.harvard.econcs.turkserver.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class CombinedHITWorkerGroup implements HITWorkerGroup {

	final HITWorkerGroup[] groups;
	
	public CombinedHITWorkerGroup(HITWorkerGroup... groups) {
		this.groups = groups;
	}

	@Override
	public int groupSize() {
		int groupSize = 0;
		for( HITWorkerGroup group : groups )
			groupSize += group.groupSize();
		return groupSize;
	}

	@Override
	public boolean contains(HITWorker hitWorker) {		
		for( HITWorkerGroup group : groups )
			if( group.contains(hitWorker) ) return true;
		return false;
	}

	@Override
	public Collection<? extends HITWorker> getHITWorkers() {
		Collection<HITWorker> workers = new LinkedList<>();		
		for( HITWorkerGroup group : groups )
			workers.addAll(group.getHITWorkers());
		return workers;
	}

	@Override
	public Collection<String> getHITIds() {
		Collection<String> hitIds = new LinkedList<>();		
		for( HITWorkerGroup group : groups )
			hitIds.addAll(group.getHITIds());
		return hitIds;
	}

	@Override
	public List<String> getWorkerIds() {
		List<String> workerIds = new LinkedList<>();		
		for( HITWorkerGroup group : groups )
			workerIds.addAll(group.getWorkerIds());
		return workerIds;
	}

	@Override
	public HITWorker findByHITId(String hitId) {
		HITWorker found = null;
		for( HITWorkerGroup group : groups ) {
			if( (found = group.findByHITId(hitId)) != null ) break;
		}
		return found;
	}

}
