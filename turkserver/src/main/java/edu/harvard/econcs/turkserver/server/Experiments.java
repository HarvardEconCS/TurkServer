package edu.harvard.econcs.turkserver.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ConfigurableServerChannel.Initializer;
import org.cometd.server.authorizer.GrantAuthorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.andrewmao.misc.Utils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.api.ExperimentController;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

@Singleton
public class Experiments implements Runnable {
	// Injector for creating bean classes
	@Inject Injector injector;
		
	BayeuxServer bayeux;	
	SessionServer server;
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	static final Initializer persistent = new Initializer() {
		@Override
		public void configureChannel(ConfigurableServerChannel channel) {
			channel.setPersistent(true);
			channel.addAuthorizer(GrantAuthorizer.GRANT_PUBLISH);
		}			
	};
	
	final EventAnnotationManager manager;
	final Assigner assigner;
	final ExperimentDataTracker tracker;
	
	final Configurator configurator;
	final Class<?> expClass;
		
	final ConcurrentMap<HITWorker, String> currentExps;
	final Queue<ExperimentListener> listeners;
	
	volatile boolean isRunning = true;
	final BlockingQueue<Runnable> expEvents;
	
	@Inject
	Experiments(
			@Named(TSConfig.EXP_CLASS) Class<?> expClass,
			@Named(TSConfig.EXP_CONFIGURATOR) Configurator configurator,			
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
		this.listeners = new ConcurrentLinkedQueue<ExperimentListener>();
		
		this.expEvents = new LinkedBlockingQueue<Runnable>();
	}

	// TODO remove this hack once things are properly wired up
	public void setReferences(BayeuxServer bayeux,
			SessionServer server) {
		this.bayeux = bayeux;
		this.server = server;		
	}	
	
	void registerListener(ExperimentListener listener) {
		listeners.add(listener);
	}

	int getMinGroupSize() {
		return configurator.groupSize();	
	}

	/*
	 * TODO replace both stuff below with custom experiment scope
	 */
	
	ExperimentControllerImpl startSingle(final HITWorkerImpl hitw) {				
		
		// Create an experiment instance with specific binding to this HITWorker
		Injector child = injector.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(HITWorker.class).toInstance(hitw);
				bind(HITWorkerGroup.class).toInstance(hitw);				
				bind(ExperimentController.class).to(ExperimentControllerImpl.class);
				bind(ExperimentControllerImpl.class).in(Scopes.SINGLETON);
			}			
		});
				
		ExperimentControllerImpl cont = child.getInstance(ExperimentControllerImpl.class);
		hitw.setExperiment(cont);
		Object experimentBean = child.getInstance(expClass);
				
		startExperiment(hitw, cont, experimentBean);
		
		return cont;
	}
	
	ExperimentControllerImpl startGroup(final HITWorkerGroupImpl group) {
				
		// Create an experiment instance with specific binding to this HITWorkerGroup
		Injector child = injector.createChildInjector(new AbstractModule() {
			@Override
			protected void configure() {				
				bind(HITWorkerGroup.class).toInstance(group);				
				bind(ExperimentController.class).to(ExperimentControllerImpl.class);
				bind(ExperimentControllerImpl.class).in(Scopes.SINGLETON);				
			}			
		});
		
		ExperimentControllerImpl cont = child.getInstance(ExperimentControllerImpl.class);
		group.setExperiment(cont);
		Object experimentBean = child.getInstance(expClass);
				
		startExperiment(group, cont, experimentBean);
		
		return cont;
	}

	private void startExperiment(final HITWorkerGroup group,
			final ExperimentControllerImpl cont, Object experimentBean) {		
		
		// Initialize the experiment data
		String inputData = assigner.getAssignment(null);
		configurator.configure(experimentBean, inputData);
		
		// Create a unique ID for an experiment, based on current timestamp
		long startTime = System.currentTimeMillis();
		final String expId = Utils.getTimeString(startTime);
		String expChannel = expId.replace(" ", "_");
		
		// Register callbacks on the experiment class
		manager.processExperiment(expId, experimentBean);		
		
		/* 
		 * Create necessary channels for this experiment
		 * Note that hostServlet automatically routes these already
		 */
				
		bayeux.createIfAbsent(Codec.expChanPrefix + expChannel, persistent);
		bayeux.createIfAbsent(Codec.expSvcPrefix + expChannel, persistent);				

		/*
		 * Send experiment channel to clients to notify connection
		 * TODO this may be unnecessary, fix protocol
		 */
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("status", Codec.connectExpAck);
		data.put("channel", expChannel);

		for( HITWorker hitw : group.getHITWorkers() ) {
			try { ((HITWorkerImpl) hitw).deliverUserService(data);
			} catch (MessageException e) { e.printStackTrace();	}			
		}				
		
		// Initialize controller, which also initializes the log
		LocalSession ls = bayeux.newLocalSession(expId);
		ls.handshake();				
		cont.initialize(startTime, expId, inputData, expChannel, ls);
		
		/* Update tracking information for experiment
		 * TODO does this start directing requests to the server before it's started?
		 */
		mapWorkers(group, expId);
		tracker.newExperimentStarted(cont);					
		
		for( ExperimentListener el : listeners ) {
			el.experimentStarted(cont);
		}
				
		/*
		 * TODO this may not be enough time for every client to register channel...
		 * reconcile this with the timing stuff above
		 */
		new Thread() {
			public void run() {				
				try { Thread.sleep(1000); }
				catch (InterruptedException e) { e.printStackTrace(); }				
				manager.triggerStart(expId);				
			}
		}.start();
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

	/**
	 * Remove mappings from workers to experiments.
	 * @param group
	 * @return
	 */
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

	public void scheduleRound(final ExperimentControllerImpl expCont, final int round) {
		expEvents
		.add(new Runnable() {
			public void run() {										
				startRound( expCont, round );
			}
		});
	}
	
	private void startRound(ExperimentControllerImpl expCont, int round) {
		Object data = ImmutableMap.of(
				"status", Codec.roundStartMsg,
				"round", round);
		for( HITWorker id : expCont.group.getHITWorkers() )			
			SessionUtils.sendServiceMsg(((HITWorkerImpl) id).cometdSession.get(), data);
		
		for( ExperimentListener el : listeners)
			el.roundStarted(expCont);
		
		manager.triggerRound(expCont.getExpId(), round);
	}

	public void rcvServiceMsg(HITWorkerImpl worker, Map<String, Object> message) {
		String expId = currentExps.get(worker);		
		if( expId == null ) {
			logger.info("{} not in experiment, ignoring service message", worker);
			return;
		}
		System.out.println(message.toString());
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

	void scheduleFinishExperiment(final ExperimentControllerImpl cont) {
		expEvents
		.add(new Runnable() {
			public void run() {
				finishExperiment(cont);
			}
		});
	}
	
	private void finishExperiment(ExperimentControllerImpl cont) {
				
		manager.deprocessExperiment(cont.getExpId());
		
		// unsubscribe from and/or remove channels
		ServerChannel toRemove = null;
		if( (toRemove = bayeux.getChannel(Codec.expChanPrefix + cont.expChannel)) != null ) {
			toRemove.setPersistent(false);
		}
		if( (toRemove = bayeux.getChannel(Codec.expSvcPrefix + cont.expChannel)) != null ) {
			toRemove.setPersistent(false);
		}
	
		tracker.experimentFinished(cont);				
		
		// TODO save the log where it is supposed to be saved
		String logOutput = cont.log.getOutput();		
		
//		String filename = String.format("%s/%s %d.log", path, expFile, clients.groupSize());
//		logger.info("Trying to open file " + filename);		
		// Save to db
//		tracker.saveSessionLog(logId, data);

		// Tell clients they are done!
		for( HITWorker id : cont.group.getHITWorkers() )			
			SessionUtils.sendStatus(((HITWorkerImpl) id).cometdSession.get(), Codec.doneExpMsg);	
		
		for( ExperimentListener el : listeners ) {
			el.experimentFinished(cont);
		}

		unmapWorkers(cont.group);
		
		// TODO Count the inactive time of anyone who disconnected before finish 
//		super.finalizeInactiveTime();
			
	}
	
	public void stop() {
		isRunning = false;
		expEvents
		.add(new Runnable() {
			public void run() {}			
		});
	}
	
	@Override
	public void run() {
		while( isRunning ) {
			try { expEvents.take().run(); }
			catch (InterruptedException e) {}
		}				
	}
}
