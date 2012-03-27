package edu.harvard.econcs.turkserver.server;

import java.util.Map;

import javax.servlet.ServletException;

import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.java.annotation.Configure;
import org.cometd.java.annotation.Listener;
import org.cometd.java.annotation.Service;
import org.cometd.java.annotation.Session;
import org.cometd.server.authorizer.GrantAuthorizer;
import org.eclipse.jetty.util.log.Log;

public class DemoServlet extends SessionServlet<SimpleExperimentServer, String> {
	
	private static final long serialVersionUID = 3128535146982507267L;
	
	@Override
	public void init() throws ServletException {		
		super.init();
		                
        processor.process(new EchoRPC());        
        processor.process(new ChatService());

        bayeux.createIfAbsent("/foo/bar/baz", new ConfigurableServerChannel.Initializer()
        {
            public void configureChannel(ConfigurableServerChannel channel)
            {
                channel.setPersistent(true);
            }
        });

        if (bayeux.getLogger().isDebugEnabled())
            System.err.println(bayeux.dump());
	}

	@Service("echo")
	public static class EchoRPC
	{
	    @Session
	    private ServerSession _session;
	    
	    @Configure("/service/echo")
	    public void configureEcho(ConfigurableServerChannel channel)
	    {
	        channel.addAuthorizer(GrantAuthorizer.GRANT_SUBSCRIBE_PUBLISH);
	    }

	    @Listener("/service/echo")
	    public void doEcho(ServerSession session, ServerMessage message)
	    {
	        Map<String,Object> data = message.getDataAsMap();
	        Log.getRootLogger().info("ECHO from "+session+" "+data);
	        session.deliver(_session, message.getChannel(), data, null);
	    }
	}
	
}
