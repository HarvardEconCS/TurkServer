package edu.harvard.econcs.turkserver.analysis;

public interface RoundParseTarget {

	void roundStart(int round, String inputData);

	void roundData(long currentTime, String nextLine);

	void roundEnd(long currentTime);

}
