package edu.harvard.econcs.turkserver.util;

import java.util.concurrent.atomic.AtomicInteger;

/*
 * Thought there was an error on OS X JVM with synchronized, but no :)
 */
public class SynchronizedErrorsTest {

	static final int iters = 100;
	
	static class FakeLobby {		
		AtomicInteger inOuterMethod1 = new AtomicInteger();
		AtomicInteger inOuterMethod2 = new AtomicInteger();
		AtomicInteger inInnerMethod = new AtomicInteger();
		
		synchronized void outerMethod1() throws InterruptedException  {
			System.out.println("In outer method 1: " + inOuterMethod1.incrementAndGet());
			Thread.sleep(50);
			innerMethod();	
			Thread.sleep(50);
			inOuterMethod1.decrementAndGet();
		}
		
		synchronized void outerMethod2() throws InterruptedException {
			System.out.println("In outer method 2: " + inOuterMethod2.incrementAndGet());
			Thread.sleep(50);
			innerMethod();
			Thread.sleep(50);
			inOuterMethod2.decrementAndGet();
		}
		
		synchronized void innerMethod() throws InterruptedException {
			System.out.println("In inner method: " + inInnerMethod.incrementAndGet());
			Thread.sleep(50);
			inInnerMethod.decrementAndGet();
		}
	}
	
	static FakeLobby lobby = new FakeLobby(); 

	static class CallingThread extends Thread {
		public void run() {
			try {
				for( int i = 0; i < iters; i++ ) {
					lobby.outerMethod1();
					lobby.outerMethod2();
				}
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
			finally {}				
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		for( int i = 0; i < 100; i++ )
			new CallingThread().start();
	}

}
