package edu.harvard.econcs.turkserver.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.andrewmao.math.RandomSelection;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.server.Assigner;

/**
 * Simple assigner that just assigns items to incoming users without regard to
 * user-item uniqueness 
 * 
 * @author mao
 *
 */
@Singleton
public class RoundRobinAssigner implements Assigner {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	Map<String, CountingEntry<String>> itemCounts;	
	NavigableSet<CountingEntry<String>> assignments;
			
	@Inject
	public RoundRobinAssigner(
			@Named(TSConfig.EXP_INPUT_LIST)
			Set<String> assts,
			@Nullable
			List<Experiment> existing) {
		
		Map<String, CountingEntry<String>> stuff = Maps.newHashMap();
		for( String asst : assts )
			stuff.put(asst, new CountingEntry<String>(asst));
		this.itemCounts = Collections.unmodifiableMap(stuff);
		
		// initialize counts for each assignment
		if( existing != null ) {
			for( Experiment e : existing) {
				String id = e.getInputdata();
				if( id != null && assts.contains(id) )
					stuff.get(id).count.incrementAndGet();
			}
		}
		
		assignments = Sets.synchronizedNavigableSet(new TreeSet<CountingEntry<String>>());
		assignments.addAll(stuff.values());
	}
	
	public RoundRobinAssigner(Set<String> assignments) {
		this(assignments, null);				
	}

	@Override
	public String getAssignment() {
		return getAssignment(null, null);
	}

	/**
	 * Get an assignment for an experiment
	 * 
	 * @param exp can be null, or contain prior data
	 * @return
	 */
	@Override
	public String getAssignment(String user, String prevItem) {
		
		if( prevItem != null && assignments.contains(prevItem) ) {
			logger.info("Resending previously allocated " + prevItem);
			return prevItem;
		}		
		
		List<CountingEntry<String>> li = Lists.newLinkedList();
		
		synchronized(assignments) {
			int lowest = assignments.first().count.get();
			Iterator<CountingEntry<String>> it = assignments.iterator();
			while( it.hasNext() ) {
				CountingEntry<String> e = it.next();
				if( e.count.get() <= lowest ) li.add(e);
				else break;
			}			
		}
		
		// choose randomly among the lowest counts
		return RandomSelection.selectRandom(li).obj;		
	}
	
	@Override
	public void completeAssignment(String item) {
		if( !itemCounts.containsKey(item) ) {
			System.out.println("Warning: unexpected assignment completed: " + item);
			return;
		}
		
		// Remove entry from map, update its count, and re-insert
		CountingEntry<String> ent = itemCounts.get(item);
		
		synchronized(assignments) {
			// To prevent others from viewing an empty map in the case of 1 assignment
			assignments.remove(ent);
			ent.count.incrementAndGet();
			assignments.add(ent);	
		}
	}

	/**
	 * Increment the completed assignment record 
	 * @param item
	 */
	@Override
	public void completeAssignment(String user, String item) {
		completeAssignment(item);
	}
	
	static class CountingEntry<E extends Comparable<? super E>> {
		final E obj;
		final AtomicInteger count = new AtomicInteger(0);
		CountingEntry(E asst) { this.obj = asst; }
		public int compareTo(CountingEntry<E> other) {
			if( count.get() == other.count.get() ) return 0;
			return (count.get() < other.count.get() ? -1 : 1);
		}
	}
	
}
