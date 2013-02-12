package edu.harvard.econcs.turkserver.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import net.andrewmao.math.RandomSelection;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.schema.Round;
import edu.harvard.econcs.turkserver.server.Assigner;

/**
 * Simple assignment maker which 
 *  - equalizes the number of assignees to items
 *  - ensures that no assignee gets the same item twice
 *  - re-assigns the same assignment to an existing assignee
 * 
 * TODO this works fine for limited numbers of items, 
 * but would be much better with a Redis implementation
 * when there are a large number of items and users
 * 
 * @author mao
 *
 */
public class UserItemAssigner implements Assigner {

	Map<String, Set<String>> itemUsers;	
	NavigableSet<String> items;
	
	@Inject
	public UserItemAssigner(
			@Named(TSConfig.EXP_INPUT_LIST)	Set<String> assignables,
			@Nullable Map<Round, String> existing) {
		
		Map<String, Set<String>> stuff = Maps.newHashMap();
		for( String asst : assignables )
			stuff.put(asst, new HashSet<String>());
		
		this.itemUsers = Collections.unmodifiableMap(stuff);
		
		// initialize existing users for each item, if any
		if( existing != null ) {
			for( Map.Entry<Round, String> e : existing.entrySet() ) {
				String item = e.getKey().getInputdata();
				String user = e.getValue();
				
				if( item != null && assignables.contains(item) )
					stuff.get(item).add(user);
			}
		}
		
		Comparator<String> comp = new Comparator<String>() {
			@Override
			public int compare(String item, String otherItem) {			
				int thisSize = itemUsers.get(item).size();
				int otherSize = itemUsers.get(otherItem).size(); 
				
				// Two strings can't be equal so we just sort alphabetically in that case
				if( thisSize == otherSize )
					return item.compareTo(otherItem);
				else
					return (thisSize<otherSize ? -1 : 1);				
			}
		};
		
		items = Sets.synchronizedNavigableSet(new TreeSet<String>(comp));
		items.addAll(assignables);
	}
	
	@Override
	public String getAssignment() {		
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAssignment(String user, String prevItem) {				
		/* Give this worker a puzzle with lowest count that he has not already done
		 * don't reassign the same items to workers
		 */
				
		if( prevItem != null && 
				itemUsers.containsKey(prevItem) && 
				!itemUsers.get(prevItem).contains(user) ) {			
			return prevItem;
		}
		
		List<String> lowestItems = Lists.newLinkedList();
		
		synchronized(items) {
			int lowest = itemUsers.get(items.first()).size();			
			
			Iterator<String> itemIter = items.iterator();
			while( itemIter.hasNext() ) {
				String item = itemIter.next();
				if( itemUsers.get(item).size() <= lowest ) 
					lowestItems.add(item);
				else
					break;
			}			
		}
		
		// choose randomly among the lowest counts
		return RandomSelection.selectRandom(lowestItems);
	}

	@Override
	public void completeAssignment(String item) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void completeAssignment(String user, String item) {
		if( !itemUsers.containsKey(item) ) {
			System.out.println("Warning: unexpected assignment completed: " + item);
			return;
		}
		
		// Remove entry from map, update its count, and re-insert
		Set<String> users = itemUsers.get(item);
		
		synchronized(items) {
			// To prevent others from viewing an empty map in the case of 1 assignment
			items.remove(item);				
			users.add(user);
			items.add(item);			
		}
	}

}
