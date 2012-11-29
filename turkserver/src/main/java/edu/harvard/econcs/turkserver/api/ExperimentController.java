package edu.harvard.econcs.turkserver.api;

public interface ExperimentController {

	void start();
	
	void finish();

	long getStartTime();
	
	long getFinishTime();
	
	String getExpId();
	
}
