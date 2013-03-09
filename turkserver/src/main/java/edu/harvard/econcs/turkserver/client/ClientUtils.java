package edu.harvard.econcs.turkserver.client;

public class ClientUtils {
	
	/**
	 * Get a SessionClient that reflects a client class
	 * @param clientClass
	 * @return
	 * @throws Exception
	 */
	public static <C> SessionClient<C> getWrappedClient(Class<C> clientClass) throws Exception {
		SessionClient<C> client = new SessionClient<C>();
		client.clientWrapper = new ClientAnnotationManager<C>(client, clientClass);
		return client;
	}
		
	/**
	 * Get a GroupClient that reflects a client class
	 * @param clientClass
	 * @return
	 * @throws Exception
	 */
	public static <C> LobbyClient<C> getWrappedLobbyClient(Class<C> clientClass) throws Exception {
		LobbyClient<C> client = new LobbyClient<C>();
		client.clientWrapper = new ClientAnnotationManager<C>(client, clientClass);
		return client;
	}
}
