package edu.harvard.econcs.turkserver.server;

import java.util.HashSet;
import java.util.Set;

import edu.harvard.econcs.turkserver.client.ClientUtils;
import edu.harvard.econcs.turkserver.client.LobbyClient;

public class ClientGenerator {

	final String url;
	int count = 0;
	
	Set<LobbyClient<?>> clients = new HashSet<>();
	
	public ClientGenerator(String url) {
		this.url = url;
	}
	
	public <C> LobbyClient<C> getClient(Class<C> clientClass) throws Exception {
		LobbyClient<C> lc = ClientUtils.getWrappedLobbyClient(clientClass);
		count++;
		lc.connect(url, "HIT " + count, "Asst " + count, "Worker " + count);
		clients.add(lc);
		return lc;
	}

	public void disposeAllClients() {
		for( LobbyClient<?> lc : clients ) {
			lc.disconnect();
		}		
	}
	
}
