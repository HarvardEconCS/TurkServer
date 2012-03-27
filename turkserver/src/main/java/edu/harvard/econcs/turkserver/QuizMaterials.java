package edu.harvard.econcs.turkserver;

import java.io.Serializable;
import java.util.Map;

public abstract class QuizMaterials implements Serializable {

	private static final long serialVersionUID = -5185522851835668336L;

	/**
	 * Converts the quiz into a JSON map for transferring
	 * @return
	 */
	public abstract Map<String, Object> toData();

}
