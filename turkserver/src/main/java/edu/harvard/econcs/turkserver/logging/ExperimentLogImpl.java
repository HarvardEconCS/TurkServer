package edu.harvard.econcs.turkserver.logging;

import java.io.IOException;
import java.io.PrintStream;

import com.google.common.collect.ObjectArrays;

import net.andrewmao.misc.Utils;

public class ExperimentLogImpl implements ServerLogController {
	
	private static final String NOT_INIT_MSG = "Log not initialized! Make sure you call start() first!";
	
	long startTime;
	
	volatile long roundStartTime;
	volatile int currentRound = 0;
	
	String expId = null;
	
	StringBuffer buffer = null;
	StringBuffer roundBuffer = null;
	
	ExperimentLogImpl() {
		buffer = new StringBuffer();
		roundBuffer = new StringBuffer();
	}
		
	public void initialize(long startTime, String expId) {
		if( this.expId != null ) 
			throw new RuntimeException("initialize already called!");
		
		this.expId = expId;
		this.startTime = startTime;

		print(expId + " started");
	}

	public void startRound(int round) {
		// Clear SB and reset the reference time		
		roundBuffer.setLength(0);
		currentRound = round;		
		
		roundStartTime = System.currentTimeMillis();
		
		printf(buffer, roundStartTime - startTime, "Round %d started", round);
		printf(roundBuffer, 0, "Round %d started", round);
	}

	public void finishRound() {
		long time = System.currentTimeMillis();
		
		printf(roundBuffer, time - roundStartTime, "Round %d finished", currentRound);
		printf(buffer, time - startTime, "Round %d finished", currentRound);
	}

	@Override
	public String getRoundOutput() {		
		return roundBuffer.toString();
	}

	@Override
	public long print(String msg) {
		if( expId == null ) throw new RuntimeException(NOT_INIT_MSG);
		
		long start = currentRound > 0 ? roundStartTime: startTime;
		StringBuffer sb = currentRound > 0 ? roundBuffer : buffer;
		long time = System.currentTimeMillis();
		
		print(sb, time - start, msg);
		return time;
	}
	
	private static void print(StringBuffer sb, long timeDiff, String msg) {						
		sb.append(
				String.format("%s %s\n", Utils.clockStringMillis(timeDiff), msg)
				);		
	}
	
	@Override
	public long printf(String format, Object... args) {
		if( this.expId == null ) throw new RuntimeException(NOT_INIT_MSG);
		
		long start = currentRound > 0 ? roundStartTime: startTime;
		StringBuffer sb = currentRound > 0 ? roundBuffer : buffer;	
		long time = System.currentTimeMillis();
		
		printf(sb, time - start, format, args);
		return time;
	}
	
	private static void printf(StringBuffer sb, long timeDiff, String format, Object... args) {								
		sb.append(
				String.format("%s " + format + "\n", ObjectArrays.concat(
						Utils.clockStringMillis(timeDiff), args)
						));		
	}
	
	@Override
	public long conclude() {
		long time = System.currentTimeMillis();

		print(buffer, time - startTime, expId + " finished");	
		
		return time;
	}
	
	@Deprecated
	public void writeToFile(String file) throws IOException {
		// TODO: legacy method that should be moved elsewhere 
		
		PrintStream ps = null;
		try {
			ps = new PrintStream(file);
			ps.print(buffer.toString());
		}
		finally {
			if( ps != null ) ps.close();
		}		
		
	}
	
	@Override
	public String getOutput() {
		return buffer.toString();
	}

}
