package edu.harvard.econcs.turkserver.server;

import org.eclipse.jetty.util.resource.Resource;

import edu.harvard.econcs.turkserver.server.DemoServlet;
import edu.harvard.econcs.turkserver.server.SimpleExperimentServer;


public class DemoServerTest {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {		
		
		Resource[] stuff = new Resource[] {                    
				Resource.newResource("html/cometd/"),
				Resource.newResource("html/cometd-demo/target/cometd-demo-2.3.1/"),                        
				};
		
		new Thread(new SimpleExperimentServer(null, null, DemoServlet.class, stuff, 0, 8000) {
			@Override
			protected int getTotalPuzzles() {				
				return 0;
			}

			@Override
			public String stringToType(String sessionId) {				
				return sessionId;
			}			
		}).start();
		
	}

}
