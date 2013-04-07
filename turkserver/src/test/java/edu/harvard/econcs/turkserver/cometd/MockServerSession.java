package edu.harvard.econcs.turkserver.cometd;

import java.util.List;
import java.util.Set;

import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage.Mutable;
import org.cometd.bayeux.server.ServerSession;

public class MockServerSession implements ServerSession {

	public boolean isConnected = true;
	
	public String lastChannel;
	public Object lastData;
	
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() {		
		return isConnected;
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
	public void addListener(ServerSessionListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeListener(ServerSessionListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLocalSession() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public LocalSession getLocalSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deliver(Session from, Mutable message) {		
		// TODO Auto-generated method stub
	}

	@Override
	public void deliver(Session from, String channel, Object data, String id) {		
		lastChannel = channel;
		lastData = data;
		
		System.out.println(channel + ": got message" + data.toString());
	}

	@Override
	public Set<ServerChannel> getSubscriptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserAgent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getInterval() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setInterval(long interval) {
		// TODO Auto-generated method stub

	}

	@Override
	public long getTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setTimeout(long timeout) {
		// TODO Auto-generated method stub

	}

}
