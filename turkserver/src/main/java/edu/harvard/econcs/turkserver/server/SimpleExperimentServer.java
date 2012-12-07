/**
 * 
 */
package edu.harvard.econcs.turkserver.server;

import org.apache.commons.configuration.Configuration;
import org.cometd.bayeux.server.ServerSession;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

/**
 * @author mao
 * 
 * A simple experiment server supporting one-way communication via JSON
 * with javascript clients.
 * 
 * Useful for experiments that don't require interaction between clients
 *
 */
@Singleton
public final class SimpleExperimentServer extends SessionServer {	
				
	@Inject
	public SimpleExperimentServer(			
			ExperimentDataTracker tracker, 
			HITController hitCont,
			WorkerAuthenticator workerAuth,
			Experiments experiments,
			JettyCometD jetty,
			Configuration config
			) throws Exception {
		
		super(tracker, hitCont, workerAuth, experiments, jetty, config);		
		
		jetty.addServlet(SessionServlet.class, "/exp");
		
        // TODO init server extensions
	}	

	@Override
	protected HITWorkerImpl sessionAccept(ServerSession session, 
			String hitId, String assignmentId, String workerId) {
		
		/*
		 * At this point, the session is successfully authenticated, so we create an experiment
		 * TODO match up with in-progress experiments at some point 
		 */
		HITWorkerImpl hitw = super.sessionAccept(session, hitId, assignmentId, workerId);

		if( experiments.workerIsInProgress(hitw) ) {
			super.sessionReconnect(session, hitw);			
		}
		else {
			ExperimentControllerImpl exp = experiments.startSingle(hitw);	
		}				
		
		return hitw;
	}

	@Override
	protected void runServerInit() {
		logger.info("Simple server...nothing to do here");		
	}
	
	
}
