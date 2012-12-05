package edu.harvard.econcs.turkserver.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ConfigurableServerChannel.Initializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.andrewmao.misc.Utils;

import com.google.common.collect.MapMaker;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.api.ExperimentController;
import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

@Singleton
public class Experiments {
	// Injector for creating bean classes
	@Inject Injector injector;
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	static final Initializer persistent = new Initializer() {
		@Override
		public void configureChannel(ConfigurableServerChannel channel) {
			channel.setPersistent(true);
		}			
	};
	
	final EventAnnotationManager manager;
	final Assigner assigner;
	final ExperimentDataTracker tracker;
	
	final Configurator configurator;
	final Class<?> expClass;
	
	final ConcurrentMap<HITWorker, String> currentExps;
	
	@Inject
	Experiments(
			@Named(TSConfig.EXP_CLASS) Class<?> expClass,
			@Named(TSConfig.EXP_INIT) Configurator configurator,			
			Assigner assigner,
			ExperimentDataTracker tracker,
			EventAnnotationManager manager
			) {
		this.expClass = expClass;
		this.configurator = configurator;
		
		this.tracker = tracker;
		this.assigner = assigner;
		this.manager = manager;
		
		this.currentExps = new MapMaker().makeMap();
	}

	int getMinGroupSize() {
		return configurator.groupSize();	
	}
	
	int getNumInProgress() {
		return new HashSet<String>(currentExps.values()).size();
	}

	int getNumFinished() {
		// TODO Auto-generated method stub
		return 0;
	}

	ExperimentControllerImpl startSingle(final HITWorkerImpl hitw, BayeuxServer bayeux) {

		/*
		 * TODO fix this injection pattern to work properly!
		 * WTF is going on here?
		 */
		
		final ExperimentLogImpl log = new ExperimentLogImpl();
		final ExperimentControllerImpl cont = new ExperimentControllerImpl(log, hitw, this);
		
		// Create an experiment instance with specific binding to this HITWorker
		Injector child = injector.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(HITWorker.class).toInstance(hitw);
				bind(ExperimentLog.class).toInstance(log);
				bind(ExperimentController.class).toInstance(cont);
			}			
		});					
		Object experimentBean = child.getInstance(expClass);
				
		// Initialize the experiment data
		String inputData = assigner.getAssignment(null);
		configurator.configure(experimentBean, inputData);
		
		// Create a unique ID for an experiment, based on current timestamp
		long startTime = System.currentTimeMillis();
		String expId = Utils.getTimeString(startTime);
		String expChannel = expId.replace(" ", "_");
		
		// Register callbacks on the experiment class
		manager.processExperiment(expId, experimentBean);
				
		// Initialize controller, which also initializes the log
		cont.initialize(startTime, expId, expChannel, bayeux.newLocalSession(expId));		
		
		// Start experiment and record time
		mapWorkers(hitw, expId);
		tracker.newExperimentStarted(expId, hitw, cont.getStartTime());
		manager.triggerStart(expId);				
		
		return cont;
	}
	
	ExperimentControllerImpl startGroup(final HITWorkerGroupImpl group, BayeuxServer bayeux) {
		
		// TODO initialize bean
		Object experimentBean = null;
		
		// Create a unique ID for an experiment, based on current timestamp
		long startTime = System.currentTimeMillis();
		String expId = Utils.getTimeString(startTime);
		String expChannel = expId.replace(" ", "_");
		
		// Register callbacks on the experiment class
		manager.processExperiment(expId, experimentBean);
		
		/* 
		 * Create necessary channels for this experiment
		 * Note that hostServlet automatically routes these already
		 */
		
		bayeux.createIfAbsent(Codec.expChanPrefix + expChannel, persistent);
		bayeux.createIfAbsent(Codec.expSvcPrefix + expChannel, persistent);				
		
		/* Update tracking information for experiment
		 * TODO does this start directing requests to the server before it's started?
		 */
		tracker.newExperimentStarted(expId, group, startTime);
		
		// Starting the experiment sends out the appropriate notifications to clients
		manager.triggerStart(expId);		
		
		/*
		 * Send experiment channel to clients to notify connection
		 * TODO this may be unnecessary, fix protocol
		 */
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("status", Codec.connectExpAck);
		data.put("channel", Codec.expChanPrefix + expChannel);

		for( HITWorker hitw : group.getHITWorkers() ) {
			try { ((HITWorkerImpl) hitw).sendServiceMessage(data);
			} catch (MessageException e) { e.printStackTrace();	}			
		}
		
		// TODO return the actual controller
		return null;
	}
	
	private void mapWorkers(HITWorkerGroup hitw, String expId) {
		if( hitw instanceof HITWorkerImpl ) {
			currentExps.put((HITWorkerImpl) hitw, expId);
			return;
		}
		
		if( hitw instanceof HITWorkerGroupImpl ) {
			for( HITWorker worker : ((HITWorkerGroupImpl) hitw).getHITWorkers() ) {
				currentExps.put(worker, expId);
			}
			return;
		}		
	}

	private String unmapWorkers(HITWorkerGroup group) {		
		if( group instanceof HITWorkerImpl ) {
			return currentExps.remove((HITWorkerImpl) group);			
		}
		
		String expId = null;
		if( group instanceof HITWorkerGroupImpl ) {
			// TODO unit test to check that all these are the same
			for( HITWorker worker : ((HITWorkerGroupImpl) group).getHITWorkers() ) {
				expId = currentExps.remove(worker);
			}
			return expId;
		}
		
		return null;
	}

	public boolean workerIsInProgress(HITWorkerImpl hitw) {		
		return currentExps.get(hitw) != null;
	}

	public void rcvServiceMsg(HITWorkerImpl worker, Map<String, Object> message) {
		String expId = currentExps.get(worker);		
		if( expId == null ) {
			logger.info("{} not in experiment, ignoring service message", worker);
			return;
		}
		
		manager.deliverServiceMsg(expId, worker, message);
	}

	public boolean rcvBroadcastMsg(HITWorkerImpl worker, Map<String, Object> message) {
		String expId = currentExps.get(worker);		
		if( expId == null ) {
			logger.info("{} not in experiment, ignoring service message", worker);
			return false;
		}
		
		return manager.deliverBroadcastMsg(expId, worker, message);		
	}

	void workerReconnected(HITWorkerImpl worker) {
		String expId = currentExps.get(worker);		
		if( expId == null ) {
			logger.info("{} not in experiment, ignoring reconnect callback", worker);
			return;
		}
		
		manager.triggerWorkerConnect(expId, worker);
	}
	
	void workerDisconnected(HITWorkerImpl worker) {
		String expId = currentExps.get(worker);		
		if( expId == null ) {
			logger.info("{} not in experiment, ignoring disconnect callback", worker);
			return;
		}
		
		manager.triggerWorkerDisconnect(expId, worker);
	}

	void finishExperiment(ExperimentControllerImpl cont) {
		
		manager.deprocessExperiment(cont.getExpId());
		
		// TODO save the log where it is supposed to be saved
		String logOutput = cont.log.getOutput();						
//		String filename = String.format("%s/%s %d.log", path, expFile, clients.groupSize());
//		logger.info("Trying to open file " + filename);		
		// Save to db
//		tracker.saveSessionLog(logId, data);
		
		unmapWorkers(cont.group);
		
		// TODO Count the inactive time of anyone who disconnected before finish 
//		super.finalizeInactiveTime();
		
		// TODO deregister experiment channels
	}
	
	void finishExperimentOld(ExperimentControllerImpl cont) {
		
		tracker.experimentFinished(cont.getExpId(), cont.group, cont.expFinishTime);
		
		// TODO notify the GUI stuff		
		serverGUI.finishedExperiment(cont);				
		
		// unsubscribe from and/or remove channels
		ServerChannel toRemove = null;
		if( (toRemove = bayeux.getChannel(Codec.expChanPrefix + cont.expChannel)) != null ) {
			toRemove.setPersistent(false);
		}
		if( (toRemove = bayeux.getChannel(Codec.expSvcPrefix + cont.expChannel)) != null ) {
			toRemove.setPersistent(false);
		}
		
		// Tell clients they are done!
		for( HITWorker id : cont.group.getHITWorkers() )			
			sendStatus(id, Codec.doneExpMsg);	
		
		if( completedHITs.addAndGet(expServer.clients.keySet().size()) >= hitGoal ) {
			logger.info("Goal of " + hitGoal + " users reached!");
			
			if( turkHITs != null ) turkHITs.expireRemainingHITs();
			
			// Only notify people in experiment, not lobby (experiment people need to submit)
			for( String id : lobbyStatus.keySet() )
				sendStatus(id, Codec.batchFinishedMsg);
			
			// TODO quit the thread in this case
		}
	}
}
