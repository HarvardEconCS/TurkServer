package edu.harvard.econcs.turkserver.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UserItemMatcherTest {

	String item = "item";
	
	UserItemMatcher<String, String> matcher;
	
	@Before
	public void setUp() throws Exception {
		Set<String> items = Collections.singleton(item);
		
		matcher = new UserItemMatcher<>(items, UserItemMatcherRandomizedTest.comp);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testReturn() {
		String user = "some user";
		
		assertEquals(0, matcher.orderedItems.first().count);		
		
		matcher.getNewAssignment(user);
		
		assertEquals(item, matcher.currentAssignments.get(user));
		assertTrue(matcher.itemUsers.get(item).contains(user));
		assertEquals(1, matcher.orderedItems.first().count);
		
		matcher.returnAssignment(user, item);
		
		assertEquals(null, matcher.currentAssignments.get(user));
		assertTrue(matcher.itemUsers.get(item).isEmpty());
		assertEquals(0, matcher.orderedItems.first().count);		
	}
	
	@Test
	public void testComplete() {
		String user = "some user";
		
		assertEquals(0, matcher.orderedItems.first().count);		
		
		matcher.getNewAssignment(user);
		
		assertEquals(item, matcher.currentAssignments.get(user));
		assertTrue(matcher.itemUsers.get(item).contains(user));
		assertEquals(1, matcher.orderedItems.first().count);
		
		matcher.completeAssignment(user, item);
		
		assertEquals(null, matcher.currentAssignments.get(user));
		assertTrue(matcher.itemUsers.get(item).contains(user));
		assertEquals(1, matcher.orderedItems.first().count);		
	}

}
