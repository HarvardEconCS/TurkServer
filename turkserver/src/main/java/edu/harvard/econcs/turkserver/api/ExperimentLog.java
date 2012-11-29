package edu.harvard.econcs.turkserver.api;

public interface ExperimentLog {

	void print(String msg);
	
	void printf(String format, Object... args);
		
}
