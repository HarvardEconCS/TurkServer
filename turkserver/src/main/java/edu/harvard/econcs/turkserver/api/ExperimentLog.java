package edu.harvard.econcs.turkserver.api;

public interface ExperimentLog {

	long print(String msg);
	
	long printf(String format, Object... args);
	
}
