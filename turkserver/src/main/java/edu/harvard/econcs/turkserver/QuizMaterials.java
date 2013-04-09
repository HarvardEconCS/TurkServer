package edu.harvard.econcs.turkserver;

import java.io.Serializable;

public abstract class QuizMaterials implements Serializable {

	private static final long serialVersionUID = -5185522851835668336L;

	/**
	 * Converts the quiz into a JSON map for transferring
	 * @return an object that must be serializable by Jetty
	 */
	public abstract Object toData();

}
