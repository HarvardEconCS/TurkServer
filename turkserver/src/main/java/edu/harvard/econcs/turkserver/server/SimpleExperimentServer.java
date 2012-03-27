/**
 * 
 */
package edu.harvard.econcs.turkserver.server;

import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import edu.harvard.econcs.turkserver.Codec.LoginStatus;
import edu.harvard.econcs.turkserver.mturk.TurkHITManager;
import edu.harvard.econcs.turkserver.server.mysql.SimpleDataTracker;

/**
 * @author mao
 * 
 * A simple experiment server supporting one-way communication via JSON
 * with javascript clients.
 * 
 * Useful for experiments that don't require interaction between clients
 *
 */
public abstract class SimpleExperimentServer extends SessionServer<String> implements Runnable {	
				
	protected SimpleDataTracker tracker;
	
	public SimpleExperimentServer(
			SimpleDataTracker userTracker,
			TurkHITManager<String> thm,
			Class<? extends SessionServlet<?, String>> servletClass,
			Resource[] resources,
			int hitGoal,
			int httpPort) throws Exception {
		
		super(userTracker, thm, resources, hitGoal, httpPort);
		
		this.tracker = userTracker;		
								
        ServletHolder experiment = context.addServlet(servletClass, "/exp");  
        experiment.setInitOrder(3);
        
        // TODO init server extensions
	}
	
	@Override
	protected String logFlush(String logId) {
		// TODO Auto-generated method stub
		String data = super.logFlush(logId);
		
//		Save to file
//		String path = NPuzzleSettings.config.getString(NPuzzleSettings.RESULT_DIR);
//		String filename = path + "/" + sessionId.toString(16) + ".log.gz";
//		FileObjUtils.writeToGZIP(filename, data);
		
		// Save to db
		tracker.saveSessionLog(logId, data);
		
		return data;
	}

	protected abstract int getTotalPuzzles();

	@Override
	protected LoginStatus sessionAccept(String clientId, String hitId,
			String assignmentId, String workerId) {
		LoginStatus status = super.sessionAccept(clientId, hitId, assignmentId, workerId);
		
		if( status != LoginStatus.ERROR ) super.logReset(hitId);
		
		return status;
	}		
	
	
}
