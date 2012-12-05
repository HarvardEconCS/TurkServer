package edu.harvard.econcs.turkserver.server;

import java.util.HashMap;
import java.util.Map;

import org.cometd.bayeux.server.ServerSession;

import com.google.common.collect.ImmutableMap;

import edu.harvard.econcs.turkserver.Codec;

public class SessionUtils {

	static void sendStatus(ServerSession session, String status, String msg) {
		Map<String, Object> errorMap = new HashMap<String, Object>();
		
		if( status != null ) errorMap.put("status", status);
		else errorMap.put("status", Codec.connectErrorAck);
		
		errorMap.put("msg", msg);
						
		sendServiceMsg(session, errorMap);
	}
	
	static void sendStatus(ServerSession session, String status) {
		Map<String, String> data = null;
		
		if( status != null ) 
			data = ImmutableMap.of("status", status);
		else
			data = ImmutableMap.of("status", Codec.connectErrorAck);				
						
		sendServiceMsg(session, data);
	}	
	
	static void sendServiceMsg(ServerSession session, Object data) {
		session.deliver(session, "/service/user", data, null);
	}
	
}
