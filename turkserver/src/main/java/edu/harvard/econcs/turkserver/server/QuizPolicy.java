package edu.harvard.econcs.turkserver.server;

import java.util.List;

import com.google.inject.Singleton;

import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.schema.Quiz;

public abstract class QuizPolicy {
		
	public abstract boolean quizPasses(QuizResults results);
	
	public abstract boolean requiresQuiz(List<Quiz> pastResults);

	/**
	 * Returns whether a user should be locked out depending on their past results
	 * @return
	 */
	public abstract boolean overallFail(List<Quiz> pastResults);

	public static QuizPolicy getDefaultQuizMaster() {		
		return new QuizPolicy() {
			
			@Override
			public boolean quizPasses(QuizResults results) {
				// Get everything right, biatch!
				return (results.correct == results.total);
			}
			
			@Override
			public boolean requiresQuiz(List<Quiz> pastResults) {
				// Require quiz unless there is one where everything is right
				for( Quiz result : pastResults ) {
					if( result.getNumCorrect() == result.getNumTotal()) return false;
				}				
				return true;
			}

			@Override
			public boolean overallFail(List<Quiz> pastResults) {
				// If someone has done more than 4 questions and less than 70% right, lock them out
				int numCorrect = 0;
				int numTotal = 0;
				
				for( Quiz result : pastResults ) {
					numCorrect += result.getNumCorrect();
					numTotal += result.getNumTotal();
				}							
				
				return numTotal > 4 && (1.0 * numCorrect / numTotal) < 0.70;
			}
			
		};
	}

	
}
