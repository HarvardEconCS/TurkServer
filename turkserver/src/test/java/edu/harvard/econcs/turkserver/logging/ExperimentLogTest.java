package edu.harvard.econcs.turkserver.logging;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExperimentLogTest {
	
	ExperimentLogImpl log;

	@Before
	public void setUp() throws Exception {
		log = new ExperimentLogImpl();
		log.initialize(System.currentTimeMillis(), "test");				
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testNoRounds() {
		System.out.println("No rounds:");
		
		log.print("Something happened");
		log.conclude();
		
		String output = log.getOutput();
				
		System.out.println(output);
		assertEquals(3, output.split("\n").length);
	}
	
	@Test(expected=RuntimeException.class)
	public void testInitError() {
		log.initialize(System.currentTimeMillis(), "test2");
	}
	
	@Test
	public void testTwoRounds() {
		System.out.println("Two rounds:");
		
		log.startRound(1);		
		log.print("Round 1 happened");
		log.finishRound();
		
		String round1 = log.getRoundOutput();
		System.out.println(round1);
		assertEquals(3, round1.split("\n").length);
		
		log.startRound(2);		
		log.print("Round 2 happened");
		log.finishRound();
		
		String round2 = log.getRoundOutput();
		System.out.println(round2);
		assertEquals(3, round2.split("\n").length);
		
		log.conclude();
		
		String output = log.getOutput();
		
		System.out.println(output);
		assertEquals(6, output.split("\n").length);
	}

}
