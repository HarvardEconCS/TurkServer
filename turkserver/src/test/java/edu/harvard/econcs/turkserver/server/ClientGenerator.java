package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.client.ClientUtils;
import edu.harvard.econcs.turkserver.client.LobbyClient;

public class ClientGenerator {

	final String url;
	int count = 0;
	
	public ClientGenerator(String url) {
		this.url = url;
	}
	
	public <C> LobbyClient<C> getClient(Class<C> clientClass) throws Exception {
		LobbyClient<C> lc = ClientUtils.getWrappedLobbyClient(clientClass);
		count++;
		lc.connect(url, "HIT " + count, "Asst " + count, "Worker " + count);
		return lc;
	}
	
}
