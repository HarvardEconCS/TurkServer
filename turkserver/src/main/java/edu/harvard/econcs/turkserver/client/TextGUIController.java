package edu.harvard.econcs.turkserver.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.harvard.econcs.turkserver.QuizMaterials;
import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.api.ClientLobbyController;

public class TextGUIController extends GUIController {
		
	Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	public TextGUIController(ClientLobbyController client) {	
		super(client);
	}

	@Override
	public void popMsg(String str) {		
		logger.info("Message: " + str);
	}
	
	@Override
	public void popMsg(String str, String title, int messageType) {
		logger.info(title + ": " + str);
	}
	
	@Override
	public void setStatus(String status) {		
		logger.info("Status: " + status);
	}
	
	@Override
	public void blankRedraw(String message) {
		logger.info("Blank: " + message);
	}
	
	@Override
	public void lobbyRedraw() {
		logger.info("Entered Lobby.");
	}
	
	@Override
	public void experimentRedraw() {
		logger.info("Entered Experiment.");		
	}

	@Override
	protected void addExperimentComponents() {}

	@Override
	public QuizResults doQuiz(QuizMaterials qm) { return null; }

}
