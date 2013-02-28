package edu.harvard.econcs.turkserver.server;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.api.IntervalEvent;
import edu.harvard.econcs.turkserver.cometd.FakeLocalSession;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;

@Singleton
public class Experiments {
	// Injector for creating bean classes
	@Inject Injector injector;
		
	BayeuxServer bayeux;		
	
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
		
	final ScheduledExecutorService eventScheduler;	
	final ConcurrentMap<String, List<ScheduledFuture<?>>> scheduledIntervals;

	@Inject
	Experiments(
			@Named(TSConfig.EXP_CLASS) Class<?> expClass,
			@Named(TSConfig.EXP_CONFIGURATOR) Configurator configurator,			
			Assigner assigner,
			ExperimentDataTracker tracker,
			EventAnnotationManager manager
			) {
		MapMaker concMapMaker = new MapMaker();
		
		this.expClass = expClass;
		this.configurator = configurator;
		
		this.tracker = tracker;
		this.assigner = assigner;
		this.manager = manager;
		
		this.currentExps = concMapMaker.makeMap();
		this.listeners = new ConcurrentLinkedQueue<ExperimentListener>();
		
		this.eventScheduler = Executors.newScheduledThreadPool(1);
		this.scheduledIntervals = concMapMaker.makeMap();
	}

	// TODO remove this hack once things are properly wired up
	public void setReferences(BayeuxServer bayeux) {
		this.bayeux = bayeux;		
	}	
	
	void registerListener(ExperimentListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Injects classes for a single-worker experiment
	 * @param hitw
	 * @return
	 */
	ExperimentControllerImpl startSingle(final HITWorkerImpl hitw) {						
		ExperimentControllerImpl cont = null;
		Object experimentBean = null;
		
		ThreadLocalScope scope = injector.getInstance(ThreadLocalScope.class); 		
		scope.enter();
		
		try {
			// Create an experiment instance with specific binding to this HITWorker		
			scope.seed(HITWorker.class, hitw);
			scope.seed(HITWorkerGroup.class, hitw);

							
			injector.getInstance(ExperimentControllerImpl.class);
			hitw.setExperiment(cont);
			injector.getInstance(expClass);
		}
		finally {
			scope.exit();
		}
		
		startExperiment(hitw, cont, experimentBean);
		
		return cont;
	}
	
	/**
	 * Injects classes for a group experiment.
	 * @param group
	 * @return
	 */
	ExperimentControllerImpl startGroup(final HITWorkerGroupImpl group) {
		ExperimentControllerImpl cont = null;
		Object experimentBean = null;
		
		ThreadLocalScope scope = injector.getInstance(ThreadLocalScope.class); 		
		scope.enter();
		
		try {
			// Create an experiment instance with specific binding to this HITWorkerGroup
			scope.seed(HITWorkerGroup.class, group);					

			cont = injector.getInstance(ExperimentControllerImpl.class);
			group.setExperiment(cont);
			experimentBean = injector.getInstance(expClass);				
		} finally {
			scope.exit();
		}
		startExperiment(group, cont, experimentBean);
		
		return cont;
	}

	void startExperiment(final HITWorkerGroup group,
			final ExperimentControllerImpl cont, Object experimentBean) {		
		
		// Initialize the experiment data
		String inputData = assigner.getAssignment();
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
		LocalSession ls = null;
		if( bayeux == null ) {
			logger.warn("Skipping bayeux channel creation...we'd better be in test mode!");
			ls = new FakeLocalSession();
		}
		else {
			bayeux.createIfAbsent(Codec.expChanPrefix + expChannel, persistent);
			bayeux.createIfAbsent(Codec.expSvcPrefix + expChannel, persistent);
			ls = bayeux.newLocalSession(expId);
			ls.handshake();	
		}
		
		/*
		 * Send experiment channel to clients to notify connection
		 * TODO this may be unnecessary, fix protocol
		 */
		Map<String, Object> data = ImmutableMap.of(
				"status", Codec.status_connectexp,
				"channel", (Object) expChannel);

		for( HITWorker hitw : group.getHITWorkers() ) {
			try { ((HITWorkerImpl) hitw).deliverUserService(data);
			} catch (MessageException e) { e.printStackTrace();	}			
		}				
		
		// Initialize controller, which also initializes the log			
		cont.initialize(startTime, expId, inputData, expChannel, ls);
		
		/* Update tracking information for experiment
		 * TODO does this start directing requests to the server before it's started?
		 */
		mapWorkers(group, expId);
		tracker.newExperimentStarted(cont);					
		
		for( ExperimentListener el : listeners ) {
			el.experimentStarted(cont);
		}

		int initialDelayMillis = 1000;
		
		/*
		 * TODO this may not be enough time for every client to register channel...
		 * reconcile this with the timing stuff above
		 * fix how clients find channels
		 */
		eventScheduler.schedule(new Runnable() {
			public void run() {				
				manager.triggerStart(expId);				
			}
		}, initialDelayMillis, TimeUnit.MILLISECONDS);
		
		// Schedule interval tasks
		List<ScheduledFuture<?>> scheduled = new LinkedList<>();
		List<Method> intervals = manager.getIntervalEvents(expId);		
		for( final Method method : intervals ) {
			IntervalEvent ie = method.getAnnotation(IntervalEvent.class);  
			ScheduledFuture<?> f = eventScheduler.scheduleAtFixedRate(new Runnable() {				
				public void run() {
					manager.triggerInterval(expId, method);				
				}				
			},
			ie.unit().convert(initialDelayMillis, TimeUnit.MILLISECONDS) + ie.interval(), 
			ie.interval(), ie.unit());
			scheduled.add(f);
		}
		scheduledIntervals.put(expId, scheduled);
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
		eventScheduler.schedule(new Runnable() {
			public void run() {										
				startRound( expCont, round );
			}
		},
		0, TimeUnit.MILLISECONDS);
	}
	
	private void startRound(ExperimentControllerImpl expCont, int round) {
		// TODO use proper start time here
		tracker.experimentRoundStarted(expCont, System.currentTimeMillis());
		
		Object data = ImmutableMap.of(
				"status", Codec.roundStartMsg,
				"round", round);
		for( HITWorker id : expCont.group.getHITWorkers() )			
			SessionUtils.sendServiceMsg(((HITWorkerImpl) id).cometdSession.get(), data);
		
		for( ExperimentListener el : listeners)
			el.roundStarted(expCont);
		
		manager.triggerRound(expCont.getExpId(), round);
	}

	public void saveLogRound(ExperimentControllerImpl expCont, String roundLog) {
		/* save log results for this round
		 * TODO save proper end time		
		 */
		tracker.experimentRoundComplete(expCont, System.currentTimeMillis(), roundLog);		
	}

	public void setBonusAmount(HITWorkerImpl hitWorker, double amount) {
		tracker.saveBonusAmount(hitWorker, amount);		
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

	void scheduleFinishExperiment(final ExperimentControllerImpl cont) {
		eventScheduler.schedule(new Runnable() {
			public void run() {
				finishExperiment(cont);
			}
		},
		0, TimeUnit.MILLISECONDS);
	}
	
	private void finishExperiment(ExperimentControllerImpl cont) {
		// Cancel all scheduled interval tasks
		List<ScheduledFuture<?>> scheduled = scheduledIntervals.remove(cont.getExpId());
		for( ScheduledFuture<?> future : scheduled )
			future.cancel(true);
		
		// Tell clients they are done!
		for( HITWorker id : cont.group.getHITWorkers() )			
			SessionUtils.sendStatus(((HITWorkerImpl) id).cometdSession.get(), Codec.doneExpMsg);	
		
		manager.deprocessExperiment(cont.getExpId());
		
		// unsubscribe from and/or remove channels
		if( bayeux == null )
			logger.warn("Skipping bayeux channel destruction...we'd better be in test mode!");					
		else {
			ServerChannel toRemove = null;
			if( (toRemove = bayeux.getChannel(Codec.expChanPrefix + cont.expChannel)) != null ) {
				toRemove.setPersistent(false);
			}
			if( (toRemove = bayeux.getChannel(Codec.expSvcPrefix + cont.expChannel)) != null ) {
				toRemove.setPersistent(false);
			}
		}

		// save the log to db
		String logOutput = cont.log.getOutput();
		tracker.experimentFinished(cont, logOutput);					
		
//		String filename = String.format("%s/%s %d.log", path, expFile, clients.groupSize());
//		logger.info("Trying to open file " + filename);		
		
		for( ExperimentListener el : listeners ) {
			el.experimentFinished(cont);
		}

		unmapWorkers(cont.group);
		
		// TODO Count the inactive time of anyone who disconnected before finish 
//		super.finalizeInactiveTime();
			
	}
	
	public ScheduledExecutorService stop() {
		eventScheduler.shutdown();
		return eventScheduler;
	}

}
