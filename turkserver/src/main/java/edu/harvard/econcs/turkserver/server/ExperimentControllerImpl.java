package edu.harvard.econcs.turkserver.server;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerSession;

import com.google.inject.Inject;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.api.ExperimentController;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

/**
 * Simple controls for an experiment
 * 
 * @author mao
 *
 */
public class ExperimentControllerImpl implements ExperimentController {

	final ServerExperimentLog log;
	final HITWorkerGroup group;	
	final Experiments experiments;	
	
	private String experimentId;
	String inputData;
	String expChannel;
	private LocalSession source;
	
	protected volatile long expStartTime;
	protected volatile long expFinishTime;
	
	protected AtomicInteger currentRound = null;
	
	@Inject
	public ExperimentControllerImpl(
			ServerExperimentLog log,
			HITWorkerGroup group,
			Experiments experiments) {
		this.log = log;
		this.group = group;
		this.experiments = experiments;		
	}
	
	void initialize(long startTime, String expId, String inputData, 
			String expChannel, LocalSession source) {
		/*
		 * TODO move this into a Guice provider?
		 * when the injection thing is fixed
		 */
		
		this.experimentId = expId;
		this.inputData = inputData;
		
		this.expChannel = expChannel;
		this.source = source;
						
		this.expStartTime = startTime;
		log.initialize(startTime, experimentId);		
	}	
	
	public void sendExperimentService(HITWorkerImpl hitWorkerImpl, Object msg) throws MessageException {
		ServerSession session = hitWorkerImpl.cometdSession.get();
		if( session == null ) throw new MessageException();
		
		session.deliver(source, Codec.expSvcPrefix + expChannel, msg, null);		
	}

	@Override
	public void sendExperimentBroadcast(Map<String, Object> msg) throws MessageException {		
		source.getChannel(Codec.expChanPrefix + expChannel).publish(msg);		
	}

	@Override
	public void startRounds() {		
		currentRound = new AtomicInteger(1);
		
		log.startRound();
		
		experiments.scheduleRound(this, currentRound.get());
	}

	@Override
	public void finishRound() {
		if( currentRound == null ) throw new RuntimeException("Not configured for rounds!");
		
		log.finishRound();
		
		experiments.scheduleRound(this, currentRound.incrementAndGet());
	}

	@Override
	public void finishExperiment() {
		this.expFinishTime = log.conclude();					
		
		experiments.finishExperiment(this);		
	}

	@Override
	public int getCurrentRound() {
		if( currentRound != null ) return currentRound.get();
		
		return 0;
	}

	@Override
	public long getStartTime() {		
		return expStartTime;
	}

	@Override
	public long getFinishTime() {
		return expFinishTime;
	}

	@Override
	public String getExpId() {
		if( experimentId == null ) throw new RuntimeException("Not initialized yet!");
		
		return experimentId;
	}

	public String getInputData() {
		if( inputData == null ) throw new RuntimeException("Not initialized with input!");
		return inputData;
	}

	public HITWorkerGroup getGroup() {
		return group;
	}
}
