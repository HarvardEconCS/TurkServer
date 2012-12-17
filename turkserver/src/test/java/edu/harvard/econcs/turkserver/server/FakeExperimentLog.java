package edu.harvard.econcs.turkserver.server;

public class FakeExperimentLog implements ServerExperimentLog {	
	
	int round;
	
	@Override
	public void initialize(long startTime, String experimentId) {
		round = 0;		
	}

	@Override
	public long print(String msg) {		
		System.out.println(msg);
		return System.currentTimeMillis();
	}

	@Override
	public long printf(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
		return System.currentTimeMillis();
	}

	@Override
	public void startRound() {
		System.out.println("Starting round " + ++round);		
	}

	@Override
	public void finishRound() {
		System.out.println("Finishing round " + round);	
	}

	@Override
	public long conclude() {
		System.out.println("Log finished.");
		return System.currentTimeMillis();
	}

	@Override
	public String getOutput() {		
		return "Nothing, output was printed to screen.";
	}

}
