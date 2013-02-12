package edu.harvard.econcs.turkserver.util;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.andrewmao.math.RandomSelection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UserItemAssignerTest {

	static int numItems = 100;
	static int numUsers = 100;
	static int repetition = 5;
	
	static int arrivals = numItems * repetition;	
	
	Set<String> items;
	Set<String> users;
	UserItemAssigner assigner;
	
	@Before
	public void setUp() throws Exception {
		items = new HashSet<String>();
		users = new HashSet<String>();
		
		for( int i = 0; i < numItems; i++ ) {
			items.add("Item " + i);
		}
		
		for( int i = 0; i < numUsers; i++ ) {
			users.add("User " + i);
		}
		
		assigner = new UserItemAssigner(items, null);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		Map<String, String> busyUsers = new HashMap<String, String>();
		Set<String> availableUsers = new HashSet<String>(users);
		
		for( int i = 0; i < arrivals; ) {
			if( Math.random() < 0.5 ) {
				// Assign a random user to a task
				String user = RandomSelection.selectRandom(availableUsers);
				if( user == null ) continue;
				
				String item = assigner.getAssignment(user, null);
				System.out.println(user + " started " + item);
				
				availableUsers.remove(user);
				busyUsers.put(user, item);
				i++;
			}
			else {
				// Have a random user complete a task
				Map.Entry<String, String> e = RandomSelection.selectRandom(busyUsers.entrySet());
				if( e == null ) continue;				
				
				String user = e.getKey();
				String item = e.getValue();
				assigner.completeAssignment(user, item);
				System.out.println(user + " completed " + item);
				
				busyUsers.remove(user);
				availableUsers.add(user);
			}
		}
		
		// Have all busy users finish tasks
		for( Iterator<Map.Entry<String, String>> it = busyUsers.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, String> e = it.next();
			it.remove();
			
			String user = e.getKey();
			String item = e.getValue();
			assigner.completeAssignment(user, item);
			System.out.println(user + " completed " + item);
			
			busyUsers.remove(user);
			availableUsers.add(user);
		}
		
		int totalAssignments = 0;
		
		for( Entry<String, Set<String>> itemUsers : assigner.itemUsers.entrySet() ) {
			// Check that each item was done the same number of times
			int nTimesDone = itemUsers.getValue().size();
			System.out.println(itemUsers.getKey() + ": " + nTimesDone);						
			totalAssignments += nTimesDone;
		}
		
		// This should ensure no one was assigned to the same item twice
		assertEquals(numItems * repetition, totalAssignments);
		
		for( Entry<String, Set<String>> itemUsers : assigner.itemUsers.entrySet() ) {
			// Check that each item was done the same number of times
			System.out.println("Checking " + itemUsers.getKey());
			int nTimesDone = itemUsers.getValue().size();
			assertEquals(repetition, nTimesDone);
		}		
		
	}

}
