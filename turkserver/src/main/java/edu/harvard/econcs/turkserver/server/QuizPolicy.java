package edu.harvard.econcs.turkserver.server;

import java.util.Collection;

import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.schema.Quiz;

public interface QuizPolicy {
		
	/**
	 * Whether a particular quiz result passes.
	 * @param results
	 * @return
	 */
	boolean quizPasses(QuizResults results);
	
	/**
	 * Whether a user requires a quiz based on their past results in this set.
	 * @param pastResults
	 * @return
	 */
	boolean requiresQuiz(Collection<Quiz> pastResults);

	/**
	 * Returns whether a user should be locked out depending on their past results
	 * @return
	 */
	boolean overallFail(Collection<Quiz> pastResults);

	public static class DefaultQuizPolicy implements QuizPolicy {		
		
		@Override
		public boolean quizPasses(QuizResults results) {
			// Get everything right, biatch!
			return (results.correct == results.total);
		}

		@Override
		public boolean requiresQuiz(Collection<Quiz> pastResults) {
			// Require quiz unless there is one where everything is right
			for( Quiz result : pastResults ) {
				if( result.getNumCorrect() == result.getNumTotal()) return false;
			}				
			return true;
		}

		@Override
		public boolean overallFail(Collection<Quiz> pastResults) {
			// If someone has done more than 4 questions and less than 70% right, lock them out
			int numCorrect = 0;
			int numTotal = 0;

			for( Quiz result : pastResults ) {
				numCorrect += result.getNumCorrect();
				numTotal += result.getNumTotal();
			}							

			return numTotal > 4 && (1.0 * numCorrect / numTotal) < 0.70;
		}

	}
	
}
