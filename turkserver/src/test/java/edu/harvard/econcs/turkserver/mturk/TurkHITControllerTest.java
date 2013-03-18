package edu.harvard.econcs.turkserver.mturk;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.andrewmao.math.RandomSelection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.mturk.TurkHITController.CreateTask;
import edu.harvard.econcs.turkserver.schema.Session;
import edu.harvard.econcs.turkserver.server.mysql.MockDataTracker;

public class TurkHITControllerTest {

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void testAdaptiveMath() {
		int num = 400;
		int min = 10;
		int max = 50;
		double overhead = 0.15;
		CreateTask someTask = new CreateTask(num, min, max, 0, 0, overhead);
		
		for( int i = 0; i <= 400; i++ ) {
			int target = TurkHITController.getAdaptiveTarget(i, someTask);						
			
			assertTrue(target >= i + min);
			assertTrue(target <= i + max);
			
			assertTrue(target == i + min || target == i + max || target == (int) Math.round(i * (1+overhead) ) );
		}	
	}

	@Test
	public void testHITCreation() {
		MockDataTracker mockTracker = new MockDataTracker();
		MockRequesterService mockReq = new MockRequesterService();
		
		TurkHITController hits = new TurkHITController(mockReq, mockTracker);
		Thread runner = new Thread(hits);
		runner.start();
		
		int num = 200;
		int min = 10;
		int max = 50;
		int delay = 2;
		int maxdelay = 1000;
		double overhead = 0.1;		
		
		Set<String> unusedHITs = Collections.synchronizedSet(new HashSet<String>());
		Set<String> takenHITs = new HashSet<String>();
		Set<String> completedHITs = new HashSet<String>();
		
		mockReq.setCreationSet(unusedHITs);
		hits.postBatchHITs(num, min, max, delay, maxdelay, overhead);
		
		// Randomly accept, return, and complete HITs
		do {
			double action = Math.random();		
			
			/*
			 * TODO: this is not updating the mappings in mocktracker
			 * but doesn't matter for this
			 */
			if( action < 0.4 ) {
				// take a random HIT
				String hit = RandomSelection.selectRandom(unusedHITs);
				if( hit == null ) continue;
				unusedHITs.remove(hit);
								
				takenHITs.add(hit);				
				
				// Let's just make sure it's been stored
				try { Thread.sleep(delay); }
				catch (InterruptedException e) {}
				Session s = mockTracker.getStoredSessionInfo(hit);
				
				synchronized(mockTracker) {
					s.setWorkerId("Worker for " + hit);		
				}
			}
			else if (action > 0.6) {
				// complete a random HIT				
				String hit = RandomSelection.selectRandom(takenHITs);
				if( hit == null ) continue;
				takenHITs.remove(hit);
								
				completedHITs.add(hit);
				Session s = mockTracker.getStoredSessionInfo(hit);
				
				synchronized(mockTracker) {
					s.setInactivePercent(0d);
				}
			}
			else {
				// return a random HIT
				String hit = RandomSelection.selectRandom(takenHITs);
				if( hit == null ) continue;
				takenHITs.remove(hit);
								
				unusedHITs.add(hit);
				Session s = mockTracker.getStoredSessionInfo(hit);
				
				synchronized(mockTracker) {
					s.setWorkerId(null);
				}
			}
			
			try { Thread.sleep(4*delay); }
			catch (InterruptedException e) {}
			
			// Check that hit posting thread is being proper			
			int unusedSize = unusedHITs.size();
			int takenSize = takenHITs.size();
			int completedSize = completedHITs.size();
			
			int createdHITs = unusedSize + takenSize + completedSize;
			int acceptedHITs = takenSize + completedSize;
			
			System.out.printf("created: %d, accepted/completed: %d\n", createdHITs, acceptedHITs);
			
			assertTrue(createdHITs >= acceptedHITs + min || createdHITs <= min);
			assertTrue(createdHITs <= acceptedHITs + max);
			
			assertTrue(
					createdHITs == acceptedHITs + min || createdHITs <= min || 
					createdHITs <= acceptedHITs + max || 
					createdHITs >= (int) Math.round(acceptedHITs * (1+overhead) ) // Inequality due to returned HITs 
					);
		} while ( completedHITs.size() < num );	
		
		// Verify that disabled HITs are what is left in unused/taken		
		Set<String> disabledHITs = Collections.synchronizedSet(new HashSet<String>());
		mockReq.setDisableSet(disabledHITs);
		
		hits.disableAndShutdown();				
		
		try {
			runner.join();
		} catch (InterruptedException e) {			
			e.printStackTrace();
		}
		
		unusedHITs.addAll(takenHITs); // Combine two remaining sets together
		
		assertTrue(unusedHITs.containsAll(disabledHITs));
		assertTrue(disabledHITs.containsAll(unusedHITs));
	}
}
