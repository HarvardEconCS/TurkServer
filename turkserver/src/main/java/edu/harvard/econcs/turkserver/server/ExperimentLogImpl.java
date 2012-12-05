package edu.harvard.econcs.turkserver.server;

import java.io.IOException;
import java.io.PrintStream;

import net.andrewmao.misc.Utils;

import edu.harvard.econcs.turkserver.api.ExperimentLog;

public class ExperimentLogImpl implements ExperimentLog {
	
	private static final String NOT_INIT_MSG = "Log not initialized! Make sure you call start() first!";

	long startTime;
	String expId = null;
	
	StringBuffer sb = null;
		
	public void initialize(long startTime, String expId) {
		if( sb != null ) throw new RuntimeException("initialize already called!");
		
		this.expId = expId;
		this.startTime = startTime;
		
		this.sb = new StringBuffer();
		
		print(expId + " started");
	}

	public void startRound() {
		// TODO Auto-generated method stub
		
	}

	public void finishRound() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long print(String msg) {
		if( expId == null ) throw new RuntimeException(NOT_INIT_MSG);
		
		long time = System.currentTimeMillis();
		
		sb.append(
				String.format("%s %s\n", 
						Utils.clockStringMillis(time - startTime), 
						msg));
		return time;
	}

	@Override
	public long printf(String format, Object... args) {
		if( expId == null ) throw new RuntimeException(NOT_INIT_MSG);
		
		long time = System.currentTimeMillis();
		
		sb.append(
				String.format("%s " + format + "\n", 
						Utils.clockStringMillis(System.currentTimeMillis() - startTime), 
						args));
		return time;
	}
	
	public long conclude() {
		return print(expId + " finished");		
	}
	
	public void writeToFile(String file) throws IOException {
		
		PrintStream ps = null;
		try {
			ps = new PrintStream(file);
			ps.print(sb.toString());
		}
		finally {
			if( ps != null ) ps.close();
		}		
		
	}
	
	public String getOutput() {
		return sb.toString();
	}

}
