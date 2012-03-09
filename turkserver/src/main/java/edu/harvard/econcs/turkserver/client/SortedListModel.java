package edu.harvard.econcs.turkserver.client;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.AbstractListModel;

public class SortedListModel<T> extends AbstractListModel {

	private static final long serialVersionUID = 2032594302192371295L;

	private TreeSet<T> things;
	private Object[] backingArray;
	
	public SortedListModel(Comparator<T> comp) {
		things = new TreeSet<T>(comp);
		backingArray = new Object[0];
	}	
	
	@SuppressWarnings("unchecked")
	@Override
	public T getElementAt(int index) {
		return (T) backingArray[index];
	}

	@Override
	public int getSize() {
		return backingArray.length;
	}

	public void updateModel(Collection<T> newSet) {
		// Remove old things
		Iterator<T> it = things.iterator();
		while(it.hasNext()) {
			T o = it.next();
			if( !newSet.contains(o) ) it.remove();
		}
		
		// Add new things
		for(T o : newSet) {
			things.add(o);
		}
		
		backingArray = new Object[things.size()];
		int i = 0;		
		for(Object o : things) {
			backingArray[i++] = o;
		}
		
		/* TODO make this more efficient
		 * Also, does it work properly when people leave?
		 */
		super.fireContentsChanged(this, 0, backingArray.length);
	}

}
