package edu.harvard.econcs.turkserver.api;

import java.util.List;

public interface HITWorkerGroup {

	List<HITWorker> getHITWorkers();
	
	void sendBroadcast(Object msg);
	
}
