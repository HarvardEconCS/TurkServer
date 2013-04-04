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
	
	/**
	 * A simple quiz factory that always returns a constant string 
	 * @author alicexigao
	 *
	 */
	public static class StringQuizFactory extends QuizFactory {
		QuizMaterials qm;
		
		public StringQuizFactory(final String quizType) {
			qm = new QuizMaterials() {				
				private static final long serialVersionUID = 1L;
				@Override
				public Object toData() {					
					return quizType;
				}				
			};
		}
		
		@Override
		public QuizMaterials getQuiz() {			
			return qm;
		}		
		
	}
	
}
