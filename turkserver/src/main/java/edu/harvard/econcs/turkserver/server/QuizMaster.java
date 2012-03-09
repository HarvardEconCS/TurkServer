package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.QuizResults;

public abstract class QuizMaster {
		
	public abstract boolean quizPasses(QuizResults results);

	public abstract boolean overallFail(int numCorrect, int numTotal);

	public static QuizMaster getDefaultQuizMaster() {		
		return new QuizMaster() {
			
			@Override
			public boolean quizPasses(QuizResults results) {
				// Get everything right, biatch!
				return (results.correct == results.total);
			}
			
			@Override
			public boolean overallFail(int numCorrect, int numTotal) {				
				return numTotal > 4 && (1.0 * numCorrect / numTotal) < 0.70;
			}
			
		};
	}

	
}
