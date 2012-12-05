/**
 * 
 */
package edu.harvard.econcs.turkserver.server;

import org.apache.commons.configuration.Configuration;
import org.cometd.bayeux.server.ServerSession;
import org.eclipse.jetty.util.resource.Resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.harvard.econcs.turkserver.mturk.TurkHITManager;
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
			TurkHITManager thm,
			WorkerAuthenticator workerAuth,
			Experiments experiments,
			Resource[] resources,
			Configuration config
			) throws Exception {
		
		super(tracker, thm, workerAuth, experiments, resources, config);		
								       
        // TODO init server extensions
	}	

	@Override
	protected HITWorkerImpl sessionAccept(ServerSession client, 
			String hitId, String assignmentId, String workerId) {
		
		/*
		 * At this point, the session is successfully authenticated, so we create an experiment
		 * TODO match up with in-progress experiments at some point 
		 */
		HITWorkerImpl hitw = super.sessionAccept(client, hitId, assignmentId, workerId);

		ExperimentControllerImpl exp = experiments.startSingle(hitw, bayeux);
		
		return hitw;
	}

	@Override
	protected void runServerInit() {
		logger.info("Simple server...nothing to do here");		
	}
	
	
}
