package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.QuizMaterials;

public abstract class QuizFactory {

	public abstract QuizMaterials getQuiz();

	/**
	 * Used for static quiz on client-side
	 * @author alicexigao
	 *
	 */
	public static class NullQuizFactory extends QuizFactory {
		@Override
		public QuizMaterials getQuiz() {			
			return null;
		}		
	}
	
}
