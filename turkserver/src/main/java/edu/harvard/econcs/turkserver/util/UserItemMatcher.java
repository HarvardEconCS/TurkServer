package edu.harvard.econcs.turkserver.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.Nullable;

import net.andrewmao.math.RandomSelection;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Simple assignment maker which 
 *  - equalizes the number of assignees to items
 *  - ensures that no assignee gets the same item twice
 *  - re-assigns the same assignment to an existing assignee
 * 
 * Thread safe.
 * 
 * TODO this works fine for limited numbers of items, 
 * but would be much better with a Redis implementation
 * when there are a large number of items and users
 * 
 * @author mao
 *
 */
public class UserItemMatcher<U, I> {

	Map<I, Set<U>> itemUsers;
	ConcurrentHashMap<U, I> currentAssignments;
	ConcurrentSkipListSet<CountingKey> orderedItems;
	
	Comparator<I> defaultOrder;
	
	public UserItemMatcher(Set<I> items,
			final Comparator<I> defaultOrder,
			@Nullable Multimap<U, I> existing) {
		
		this.defaultOrder = defaultOrder;
		
		Map<I, Set<U>> temp = Maps.newHashMap();
		for( I asst : items ) {
			// Synchronize all sets, no concurrency needed
			temp.put(asst, Collections.synchronizedSet(new HashSet<U>()));
		}
		
		itemUsers = Collections.unmodifiableMap(temp);
		
		// initialize existing users for each item, if any
		if( existing != null ) {
			for( Map.Entry<U, I> e : existing.entries() ) {
				U user = e.getKey();
				I item = e.getValue();				
				
				if( items.contains(item) ) itemUsers.get(item).add(user);
			}
		}				
		
		// Set up counts for existing keys
		orderedItems = new ConcurrentSkipListSet<CountingKey>();
		
		for( Map.Entry<I, Set<U>> e : itemUsers.entrySet() ) {
			orderedItems.add(new CountingKey(e.getKey(), e.getValue().size()));
		}
		
		currentAssignments = new ConcurrentHashMap<U, I>();
	}
	
	public UserItemMatcher(Set<I> items, final Comparator<I> defaultOrder) {
		this(items, defaultOrder, null);
	}
		
	/**
	 * Give this worker an item with lowest count that he has not already done
	 * don't reassign the same items to workers
	 */
	public I getNewAssignment(U user) {
		
		List<CountingKey> lowestItems = Lists.newLinkedList();
				 
		int lowest = 0;		
		
		for( Iterator<CountingKey> it = orderedItems.iterator(); it.hasNext(); ) {
			CountingKey currentKey = it.next();
			
			if( currentKey.count > lowest ) {
				if( lowestItems.isEmpty() ) {
					lowest = currentKey.count;
				}
				else {
					break;
				}
			}
			
			// do not assign users to items they have already seen
			Set<U> existing = itemUsers.get(currentKey.key);
			
			if (existing.contains(user)) continue;
			
			lowestItems.add(currentKey);			
		}
		
		// choose randomly among the lowest counts
		CountingKey selected = RandomSelection.selectRandom(lowestItems);
		
		I prevAssigned = null;
		
		if( selected != null && (prevAssigned = currentAssignments.putIfAbsent(user, selected.key)) != null ) {
			System.out.printf("Warning: returning existing current assignment %s for user %s\n", prevAssigned, user);
			return prevAssigned;
		}
				
		if( lowestItems.isEmpty() ) {
			System.out.printf("Sorry, nothing to assign for user %s\n", user);
			return null;
		}	
		
		// To prevent others from viewing an empty map in the case of 1 assignment, we insert a duplicate before removing		
		CountingKey newKey = new CountingKey(selected.key, selected.count + 1);
		orderedItems.add(newKey);
		
		// Remove old key from map, and update users in set		
		orderedItems.remove(selected);		
		// TODO very rare case that user could get reassigned same item if they complete before here, but it's practically impossible
		Set<U> users = itemUsers.get(selected.key);
		users.add(user);
		
		return selected.key;
	}
	
	public I getCurrentAssignment(U user) {
		return currentAssignments.get(user);
	}

	public void returnAssignment(U user, I item) {
		Set<U> users = itemUsers.get(item);
		
		if( users == null ) {
			System.out.println("Warning: unexpected assignment to return: " + item);
			return;
		}
		
		if( !currentAssignments.remove(user, item) ) {
			throw new IllegalStateException(String.format("unexpected return of %s: %s", item, user));			
		}
		
		if( !users.remove(user) ) {
			System.out.println("Warning: user not in set. shouldn't have gotten here.");
		}
	}
	
	public void completeAssignment(U user, I item) {
		if( !itemUsers.containsKey(item) ) {
			System.out.println("Warning: unexpected assignment completed: " + item);
			return;
		}
		
		if( !currentAssignments.remove(user, item) ) {
			throw new IllegalStateException(String.format("Warning: unexpected completion of %s: %s", item, user));			
		}
	}
	
	class CountingKey implements Comparable<CountingKey> {
		I key;
		int count;
		
		CountingKey(I key, int count) {
			this.key = key;
			this.count = count;
		}

		@Override
		public int compareTo(CountingKey other) {
			// Two strings can't be equal so we just sort by some other order in that case
			if( this.count == other.count )
				return defaultOrder.compare(this.key, other.key);
			else
				return (this.count < other.count ? -1 : 1);			
		}
	}

}
