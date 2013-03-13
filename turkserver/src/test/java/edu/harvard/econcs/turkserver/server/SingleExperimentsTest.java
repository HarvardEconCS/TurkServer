package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.andrewmao.math.RandomSelection;
import net.andrewmao.misc.ConcurrentBooleanCounter;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import edu.harvard.econcs.turkserver.client.SessionClient;
import edu.harvard.econcs.turkserver.client.TestClient;
import edu.harvard.econcs.turkserver.config.DataModule;
import edu.harvard.econcs.turkserver.config.ConfigModules;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.config.TestConfigModules;
import edu.harvard.econcs.turkserver.config.TestServerModule;

public class SingleExperimentsTest {

	static final int EVENT_PUSH_MILLIS = 150;
	
	static final int numWorkers = 10;
	static final int numHITs = 40;		
	
	String url = "http://localhost:9876/cometd/";
	
	Random rnd = new Random();
		
	ConcurrentBooleanCounter<String> usedHITs;
	ConcurrentHashMap<String, String> hitToAssignment;
	
	Set<String> workers;
	
	TurkServer ts;
	ClientGenerator cg;
	
	@Before
	public void setUp() throws Exception {				
		usedHITs = new ConcurrentBooleanCounter<String>();
		hitToAssignment = new ConcurrentHashMap<String, String>();
		workers = new HashSet<String>();
		
		// Set up shit
		for( int i = 0; i < numHITs; i++ )
			usedHITs.put(RandomStringUtils.randomAlphanumeric(30), false);
		
		for( String hitId : usedHITs.keySet() )
			hitToAssignment.put(hitId, RandomStringUtils.randomAlphanumeric(30));
		
		for( int i = 0; i < numWorkers; i++ )
			workers.add(RandomStringUtils.randomAlphanumeric(14));
		
		TestUtils.waitForPort(9876);
		TestUtils.waitForPort(9877);
		
		// Make sure the ports are clear
		Thread.sleep(500);
	}
	
	class SingleTestModule extends TestServerModule {
		@Override
		public void configure() {
			super.configure();
						
			bindExperimentClass(TestExperiment.class);
			bindConfigurator(new TestConfigurator(1, 0));
			bindString(TSConfig.EXP_SETID, "test");
		}
	}

	@After
	public void tearDown() throws Exception {
		if( cg != null ) {
			cg.disposeAllClients();
		}
		if( ts != null ) {
			ts.orderlyShutdown();
		}	
	}

	@Test
	public void test() throws Exception {		
		DataModule dm = new DataModule();
		
		Configuration conf = dm.getConfiguration();		
		conf.setProperty(TSConfig.SERVER_HITGOAL, numHITs);						
		conf.setProperty(TSConfig.EXP_REPEAT_LIMIT, numHITs / numWorkers + 2);
		
		ts = new TurkServer(dm);
		
		ts.runExperiment(
				new SingleTestModule(),
				ConfigModules.SINGLE_EXPERIMENTS,
				TestConfigModules.TEMP_DATABASE,
				TestConfigModules.NO_HITS,
				TestConfigModules.SCREEN_LOGGING
				);
				
		Thread.sleep(500);

		// Start doin stuff
		Set<String> untakenHITs = new HashSet<String>(usedHITs.keySet());
		Set<String> takenHITs = new HashSet<String>();
		Multimap<String, SessionClient<TestClient>> workerToPC = HashMultimap.create();
		
		cg = new ClientGenerator(url);
		
		do {			
			double r = rnd.nextDouble();
			/*
			 * Probabilities:
			 * 0.4 someone takes (accepts) a HIT
			 * 0.4 someone submits a HIT
			 * 0.2 someone stops working (returns) a HIT 
			 */
			if( r < 0.4 ) {
				// Pick one of the unassigned HITs and assign it to someone.
				if( untakenHITs.size() == 0 ) continue;
				
				String randomWorker = RandomSelection.selectRandom(workers);
				
				// Don't take multiple HITs per worker for now.
				if( workerToPC.get(randomWorker).size() > 0 ) continue;
				
				String randomHIT = RandomSelection.selectRandom(untakenHITs);
				String randomAsst = hitToAssignment.get(randomHIT);				
				
				SessionClient<TestClient> pc = 
						cg.getSessionClient(TestClient.class, randomHIT, randomAsst, randomWorker);							
				
				untakenHITs.remove(randomHIT);
				takenHITs.add(randomHIT);
				workerToPC.put(randomWorker, pc);
				
				Thread.sleep(EVENT_PUSH_MILLIS);
				
				assertTrue(pc.isConnected());
				
				if( pc.isError() ) {
					System.out.println("Server rejected this accept, returning");
					// We need to return this HIT and close it
					cg.disposeClient(pc);
					workerToPC.remove(randomWorker, pc);
					
					String hitId = pc.getHitId();
					takenHITs.remove(hitId);
					untakenHITs.add(hitId);
				}
				else {
					Thread.sleep(EVENT_PUSH_MILLIS);
					/*
					 * check that the client got start message
					 * TODO this is giving startExp instead of broadcast
					 */
					assertNotNull(pc.getClientBean().lastCall);
//					assertEquals("broadcast", pc.getClientBean().lastCall);
				}				
			}
			else if( r < 0.9 ) {				
				// Pick a worker and complete its HIT
				if( workerToPC.size() == 0) continue;
				
				String randomWorker = RandomSelection.selectRandom(workerToPC.keySet());
				SessionClient<TestClient> pc = RandomSelection.selectRandom(
						workerToPC.get(randomWorker)); 
				
				pc.getClientBean().getController().sendExperimentBroadcast(
						ImmutableMap.<String, Object>of("msg", pc.getClientBean().toString())
						);
				
				Thread.sleep(EVENT_PUSH_MILLIS);
				
				// Should get return at this point
				assertEquals("finishExp", pc.getClientBean().lastCall);
				pc.submit("some comments");
				
				Thread.sleep(EVENT_PUSH_MILLIS);				
				// Make sure it got the complete message and disconnected
				assertTrue(!pc.isConnected());
				
				cg.disposeClient(pc);
				workerToPC.remove(randomWorker, pc);
				
				String hitId = pc.getHitId(); 
				usedHITs.put(hitId, true);
				takenHITs.remove(hitId);
				
				assertEquals(ts.getSessionServer().getNumCompleted(), usedHITs.getTrueCount());
			}
			else {
				// Pick a worker and return its HIT
				if( workerToPC.size() == 0) continue;
				
				String randomWorker = RandomSelection.selectRandom(workerToPC.keySet());
				Collection<SessionClient<TestClient>> pcs = workerToPC.get(randomWorker);
				
				assertTrue(pcs.size() <= 1);
				
				if( pcs.size() > 0) {
					SessionClient<TestClient> pc = RandomSelection.selectRandom(pcs);
					
					cg.disposeClient(pc);
					workerToPC.remove(randomWorker, pc);
					
					String hitId = pc.getHitId();
					takenHITs.remove(hitId);
					untakenHITs.add(hitId);
				}								
			}											
			
		} while (usedHITs.getTrueCount() < numHITs);
		
		assertEquals(ts.getSessionServer().getNumCompleted(), numHITs);
		
		System.out.println("finished");

	}

}
