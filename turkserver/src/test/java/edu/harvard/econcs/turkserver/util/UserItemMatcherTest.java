package edu.harvard.econcs.turkserver.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class UserItemMatcherTest {

	@Parameters
	public static Collection<Object[]> parameters() {		
		return Arrays.<Object[]>asList(
				new Object[] {10, 1, 10},
				new Object[] {50, 1000, 10},
				new Object[] {1000, 50, 5},
				new Object[] {100, 100, 5}
				);
	}

	Comparator<String> comp = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {				
			return o1.compareTo(o2);
		}		
	};
	
	int numItems, numUsers, repetition;	
	int arrivals;	
			
	public UserItemMatcherTest(int numItems, int numUsers, int repetitionPerItem) {
		this.numItems = numItems;
		this.numUsers = numUsers;
		this.repetition = Math.min(repetitionPerItem, numUsers);
		
		this.arrivals = numItems * repetition;
	}
	
	Set<String> items;
	Set<String> users;
	UserItemMatcher<String, String> assigner;	
	
	Map<String, String> busyUsers;
	Set<String> availableUsers;
	
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
		
		assigner = new UserItemMatcher<>(items, comp);
		
		busyUsers = new HashMap<String, String>();
		availableUsers = new HashSet<String>(users);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {			
		System.out.printf("Beginning test with %d items, %d users, %d arrivals\n", numItems, numUsers, arrivals);			
		
		for( int i = 0; i < arrivals; ) {
			if( Math.random() < 0.5 ) {
				// Assign a random user to a task
				String user = RandomSelection.selectRandom(availableUsers);
				if( user == null ) continue;
				
				beginAssignment(user);
				i++;
			}
			else {
				// Have a random user complete a task
				Map.Entry<String, String> e = RandomSelection.selectRandom(busyUsers.entrySet());
				if( e == null ) continue;				
								
				completeAssignment(e.getKey(), e.getValue());
			}
		}
		
		// Have all busy users finish tasks
		for( Iterator<Map.Entry<String, String>> it = busyUsers.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, String> e = it.next();
			it.remove();
			
			completeAssignment(e.getKey(), e.getValue());
		}
		
		int totalAssignments = 0;
		
		for( Entry<String, Set<String>> itemUsersEntry : assigner.itemUsers.entrySet() ) {
			// Check that each item was done the same number of times
			int nTimesDone = itemUsersEntry.getValue().size();
								
			totalAssignments += nTimesDone;
		}
		
		// This should ensure no one was assigned to the same item twice
		assertEquals(numItems, assigner.itemUsers.size());
		assertEquals(arrivals, totalAssignments);
		
		for( Entry<String, Set<String>> itemUsersEntry : assigner.itemUsers.entrySet() ) {
			// Check that each item was done the same number of times
			int nTimesDone = itemUsersEntry.getValue().size();
			
			System.out.println("Checking " + itemUsersEntry.getValue());
			
			/*
			 * TODO fix this check
			 * Occasionally fails here if a user was forced to do 
			 * some higher task due to having done a lower one before
			 * (not the fault of the algorithm)
			 */
			assertEquals(repetition, nTimesDone);						
		}
	}

	void beginAssignment(String user) {
		String item = assigner.getNewAssignment(user);
//		System.out.println(user + " started " + item);
		
		availableUsers.remove(user);
		busyUsers.put(user, item);
	}

	void completeAssignment(String user, String item) {
		assigner.completeAssignment(user, item);
//		System.out.println(user + " completed " + item);
		
		busyUsers.remove(user);
		availableUsers.add(user);
	}

}
