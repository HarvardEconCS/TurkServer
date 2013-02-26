package edu.harvard.econcs.turkserver.server;

import static com.google.common.base.Preconditions.*;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;

/**
 * Thread-local scope for creating a single experiment
 * allowing for binding the log, controller, other stuff
 * 
 * @author mao
 *
 */
public class ThreadLocalScope implements Scope {
	
	private static final Provider<Object> SEEDED_KEY_PROVIDER =
			new Provider<Object>() {
		public Object get() {
			throw new IllegalStateException("If you got here then it means that" +
					" your code asked for scoped object which should have been" +
					" explicitly seeded in this scope by calling" +
					" SimpleScope.seed(), but was not.");
		}
	};
	
	private final ThreadLocal<Map<Key<?>, Object>> values = new ThreadLocal<Map<Key<?>, Object>>();

	public void enter() {
		checkState(values.get() == null, "A scoping block is already in progress");
		values.set(Maps.<Key<?>, Object>newHashMap());
	}

	public void exit() {
		checkState(values != null, "No scoping block in progress");
		values.set(null);
	}
	
	public <T> void seed(Key<T> key, T value) {
		Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);
		checkState(!scopedObjects.containsKey(key), "A value for the key %s was " +
				"already seeded in this scope. Old value: %s New value: %s", key,
				scopedObjects.get(key), value);
		scopedObjects.put(key, value);
	}
	
	public <T> void seed(Class<T> clazz, T value) {
		seed(Key.get(clazz), value);
	}
	
	@Override
	public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
		return new Provider<T>() {
			public T get() {
				Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);

				@SuppressWarnings("unchecked")
				T current = (T) scopedObjects.get(key);
				if (current == null && !scopedObjects.containsKey(key)) {
					current = unscoped.get();
					scopedObjects.put(key, current);
				}
				return current;
			}
		};		
	}
	
	private <T> Map<Key<?>, Object> getScopedObjectMap(Key<T> key) {
		Map<Key<?>, Object> scopedObjects = values.get();
		if (scopedObjects == null) {
			throw new OutOfScopeException("Cannot access " + key
					+ " outside of a scoping block");
		}
		return scopedObjects;
	}

	public String toString() {
		return this.getClass().getSimpleName(); 
	}
	
	/**
	 * Returns a provider that always throws exception complaining that the object
	 * in question must be seeded before it can be injected.
	 *
	 * @return typed provider
	 */
	@SuppressWarnings({"unchecked"})
	public static <T> Provider<T> seededKeyProvider() {
		return (Provider<T>) SEEDED_KEY_PROVIDER;
	}

}
