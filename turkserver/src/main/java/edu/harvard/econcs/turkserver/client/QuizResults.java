package edu.harvard.econcs.turkserver.client;

import java.io.Serializable;

public class QuizResults implements Serializable {
	
	private static final long serialVersionUID = -3864495197126855741L;

	public int correct = 0;
	public int total = 0;
	
	public String answers; // TODO: be filled in by clients
	
//	public Map<String, String[]> checkedChoices;
	
	public void addCorrectResponse() {
		correct++;
		total++;
	}

	public void addIncorrectResponse() {
		total++;
	}

	/**
	 * Convert checked choices to string
	 * @return
	 */
//	public String getAnswerString() {
//		Map<String, String> mapOfStrings = new HashMap<String, String>();
//		for (String key : checkedChoices.keySet()) {
//			String[] answers = checkedChoices.get(key);
//			String answerString = Arrays.toString(answers);
//			mapOfStrings.put(key, answerString);
//		}
//		return mapOfStrings.toString();
//		
//	}
}
