package edu.harvard.econcs.turkserver.server;

import java.util.Collections;
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

	/**
	 * Updates the lobby status of a worker.
	 * @param hitw
	 * @param data
	 * @return true if the lobby status changed
	 */
	boolean updateStatus(HITWorkerImpl hitw, Map<String, Object> data);	
	
	/**
	 * Try and remove worker from lobby
	 * @param hitw
	 * @return true if the worker was removed from the lobby
	 */
	boolean userQuit(HITWorkerImpl hitw);
		
	Object getStatus(HITWorkerImpl hitw);

	Set<HITWorkerImpl> getLobbyUsers();

	public class NullLobby implements Lobby {
		
		boolean gotUsers = false; // For a unit test
	
		@Override
		public void setListener(LobbyListener listener) {}
	
		@Override
		public void setMessage(String text) {}
	
		@Override
		public void userJoined(HITWorkerImpl hitw) {
			System.out.printf("Warning: %s shouldn't have joined null lobby\n", hitw);	
		}
	
		@Override
		public boolean updateStatus(HITWorkerImpl hitw, Map<String, Object> data) {
			System.out.printf("Warning: %s shouldn't have sent status to null lobby\n", hitw);
			return false;
		}
	
		@Override
		public boolean userQuit(HITWorkerImpl hitw) {
			return false;
		}
	
		@Override
		public Object getStatus(HITWorkerImpl hitw) { 
			return null;
		}
	
		@Override
		public Set<HITWorkerImpl> getLobbyUsers() {
			gotUsers = true;
			return Collections.emptySet();
		}	
	}
}
