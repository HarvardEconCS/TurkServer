package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.api.ExperimentLog;

interface ServerExperimentLog extends ExperimentLog {

	void initialize(long startTime, String experimentId);

	void startRound(int round);

	void finishRound();

	long conclude();

	String getOutput();

}
