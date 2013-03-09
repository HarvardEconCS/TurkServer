package edu.harvard.econcs.turkserver.cometd;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerSession;

public class MockLocalSession implements LocalSession {

	@Override
	public void addExtension(Extension extension) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeExtension(Extension extension) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Extension> getExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handshake() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handshake(Map<String, Object> template) {
		// TODO Auto-generated method stub

	}

	@Override
	public ClientSessionChannel getChannel(String channelName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isHandshook() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAttribute(String name, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object removeAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void batch(Runnable batch) {
		// TODO Auto-generated method stub

	}

	@Override
	public void startBatch() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean endBatch() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ServerSession getServerSession() {
		// TODO Auto-generated method stub
		return null;
	}

}
