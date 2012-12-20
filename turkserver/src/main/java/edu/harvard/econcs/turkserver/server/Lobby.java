package edu.harvard.econcs.turkserver.server;

import java.util.Map;
import java.util.Set;

public interface Lobby {
	
	void setListener(LobbyListener listener);

	/**
	 * Set a message that should be displayed to the lobby, if any
	 * @param text
	 */
	void setMessage(String text);

	void userJoined(HITWorkerImpl hitw);

	void updateStatus(HITWorkerImpl hitw, Map<String, Object> data);	
	
	/**
	 * Try and remove worker from lobby
	 * @param hitw
	 * @return true if the worker was removed from the lobby
	 */
	boolean userQuit(HITWorkerImpl hitw);
		
	Object getStatus(HITWorkerImpl hitw);

	Set<HITWorkerImpl> getLobbyUsers();
}
