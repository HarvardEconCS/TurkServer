package edu.harvard.econcs.turkserver.server;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.cometd.MockServerSession;

public class WorkerInactivityTest {

	ExperimentControllerImpl expCont;
	MockServerSession testSession;
	HITWorkerImpl testWorker;
	
	@Before
	public void setUp() throws Exception {		
		testSession = new MockServerSession();
		testWorker = new HITWorkerImpl(testSession, null);
		
		expCont = new ExperimentControllerImpl(null, testWorker, null);
		testWorker.setExperiment(expCont);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDisconnection() throws Exception {
		long discTime = 100;
		
		assertEquals(0, testWorker.getNumDisconnects());
		assertEquals(0, testWorker.getLastInactiveTime());
		
		testSession.isConnected = false;		
		testWorker.disconnected();
		
		assertEquals(1, testWorker.getNumDisconnects());
		assertEquals(0, testWorker.getLastInactiveTime());
		
		Thread.sleep(discTime);
		
		assertEquals(discTime, testWorker.getDisconnectedTime(), 1);
		
		testSession.isConnected = true;
		testWorker.reconnected();
		
		assertEquals(-1, testWorker.getDisconnectedTime());
		assertEquals(1, testWorker.getNumDisconnects());
		assertEquals(0, testWorker.getLastInactiveTime());
		assertEquals(discTime, testWorker.getTotalInactiveTime(), 1);
		
		testSession.isConnected = false;
		testWorker.disconnected();
		
		Thread.sleep(discTime);
		
		// Set fake end time for experiment
		expCont.expFinishTime = System.currentTimeMillis();
		
		testWorker.finalizeActivity();		
		assertEquals(2*discTime, testWorker.getTotalInactiveTime(), 1);
	}
	
	@Test
	public void testInactivity() {
		long inactiveStart1 = 0;
		long inactiveStart2 = 10000;
		
		assertEquals(0, testWorker.getLastInactiveTime());
		
		testWorker.addInactiveTime(inactiveStart1, 100);
		
		assertEquals(100, testWorker.getTotalInactiveTime());
		
		testWorker.addInactiveTime(inactiveStart1, 200);
		
		assertEquals(200, testWorker.getTotalInactiveTime());
		
		testWorker.addInactiveTime(inactiveStart2, 100);
		
		assertEquals(300, testWorker.getTotalInactiveTime());
		
		testWorker.addInactiveTime(inactiveStart2, 200);
		
		assertEquals(400, testWorker.getTotalInactiveTime());
		
		testWorker.finalizeActivity();
		
		assertEquals(400, testWorker.getTotalInactiveTime());
	}

	/*
	 * Go inactive and then disconnect
	 */
	@Test
	public void testInactiveDisconnect() throws Exception {
		long inactiveStart1 = 0;
		long inactiveStart2 = 10000;
		
		long discTime = 100;
		
		testWorker.addInactiveTime(inactiveStart1, 100);
		
		assertEquals(0, testWorker.getNumDisconnects());
		assertEquals(100, testWorker.getLastInactiveTime());
		assertEquals(100, testWorker.getTotalInactiveTime());
		
		testSession.isConnected = false;		
		testWorker.disconnected();
		
		assertEquals(1, testWorker.getNumDisconnects());
		assertEquals(100, testWorker.getLastInactiveTime());
		assertEquals(100, testWorker.getTotalInactiveTime());
		
		Thread.sleep(discTime);		
		
		testSession.isConnected = true;
		testWorker.reconnected();
		
		assertEquals(1, testWorker.getNumDisconnects());
		assertEquals(100, testWorker.getLastInactiveTime());
		assertEquals(200, testWorker.getTotalInactiveTime(), 1);
		
		testWorker.addInactiveTime(inactiveStart2, 200);
		
		assertEquals(200, testWorker.getLastInactiveTime());
		assertEquals(400, testWorker.getTotalInactiveTime(), 1);
				
		expCont.expFinishTime = System.currentTimeMillis();		
		testWorker.finalizeActivity();
			
		assertEquals(400, testWorker.getTotalInactiveTime(), 1);
	}
	
	/*
	 * Test the rare case where a disconnection is followed by the same inactivity
	 * TODO fix the implementation of this at some point
	 */
	@Test
	public void testInactiveDisconnectOverlap() throws Exception {
		long inactiveStart1 = 0;		
		
		long discTime = 100;
		
		testWorker.addInactiveTime(inactiveStart1, 100);
		
		assertEquals(0, testWorker.getNumDisconnects());
		assertEquals(100, testWorker.getLastInactiveTime());
		assertEquals(100, testWorker.getTotalInactiveTime());
		
		testSession.isConnected = false;		
		testWorker.disconnected();
		
		assertEquals(1, testWorker.getNumDisconnects());
		assertEquals(100, testWorker.getLastInactiveTime());
		assertEquals(100, testWorker.getTotalInactiveTime());
		
		Thread.sleep(discTime);		
		
		testSession.isConnected = true;
		testWorker.reconnected();
		
		assertEquals(1, testWorker.getNumDisconnects());
		assertEquals(100, testWorker.getLastInactiveTime());
		assertEquals(200, testWorker.getTotalInactiveTime(), 1);
		
		testWorker.addInactiveTime(inactiveStart1, 200);
		
		assertEquals(200, testWorker.getLastInactiveTime());		
		// TODO This currently returns 300 due to extra counting the disconnect time
		assertEquals(200, testWorker.getTotalInactiveTime(), 1);
				
		expCont.expFinishTime = System.currentTimeMillis();		
		testWorker.finalizeActivity();
			
		assertEquals(200, testWorker.getTotalInactiveTime(), 1);
	}
	
}
