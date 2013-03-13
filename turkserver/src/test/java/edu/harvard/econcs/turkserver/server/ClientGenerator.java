package edu.harvard.econcs.turkserver.server;

import java.util.HashSet;
import java.util.Set;

import edu.harvard.econcs.turkserver.client.ClientUtils;
import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.client.SessionClient;
import edu.harvard.econcs.turkserver.client.TestClient;

public class ClientGenerator {

	final String url;
	int count = 0;
	
	Set<SessionClient<?>> clients = new HashSet<>();
	
	public ClientGenerator(String url) {
		this.url = url;
	}
	
	public <C> SessionClient<C> getSessionClient(Class<C> clientClass,
			String hitId, String assignmentId, String workerId) throws Exception {
		SessionClient<C> lc = ClientUtils.getWrappedClient(clientClass);
		count++;
		lc.connect(url, hitId, assignmentId, workerId);
		clients.add(lc);
		return lc;
	}
	
	public <C> LobbyClient<C> getClient(Class<C> clientClass) throws Exception {
		LobbyClient<C> lc = ClientUtils.getWrappedLobbyClient(clientClass);
		count++;
		lc.connect(url, "HIT " + count, "Asst " + count, "Worker " + count);
		clients.add(lc);
		return lc;
	}

	public void disposeClient(SessionClient<TestClient> sc) {
		sc.disconnect();		
		clients.remove(sc);
	}

	public void disposeAllClients() {
		for( SessionClient<?> lc : clients ) {
			lc.disconnect();
		}		
	}
	
}
