package edu.harvard.econcs.turkserver.api;

import java.io.IOException;

public interface ExperimentLog {

	void initialize(long startTime, String expId);
	
	void print(String msg);
	
	void printf(String format, Object... args);
	
	void conclude();

	void writeToFile(String file) throws IOException;
	
	String getOutput();
	
}
