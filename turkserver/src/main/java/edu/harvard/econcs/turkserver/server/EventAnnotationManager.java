package edu.harvard.econcs.turkserver.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Singleton;

import edu.harvard.econcs.turkserver.api.*;

/**
 * Handles callback events for experiments
 * 
 * TODO fix the triggered methods here to properly handle
 * superclass methods with the same signature, superclass methods
 * are currently added but not called at all.
 * 
 * @author mao
 *
 */
@Singleton
public class EventAnnotationManager {
		
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	final ConcurrentMap<String, Object> beans;
	final Multimap<Class<?>, Object> beanClasses;
	
	final Multimap<Class<?>, Method> starts;
	final Multimap<Class<?>, Method> rounds;
	final Multimap<Class<?>, Method> intervals;
	final Multimap<Class<?>, Method> timeouts;	
	final Multimap<Class<?>, Method> connects;
	final Multimap<Class<?>, Method> disconnects;
	
	final Multimap<Class<?>, Method> broadcasts;
	final Multimap<Class<?>, Method> services;
		
	EventAnnotationManager() {		
		beans = new MapMaker().makeMap();
		
		beanClasses = Multimaps.synchronizedSetMultimap(
				HashMultimap.<Class<?>, Object>create());	
		
		starts = Multimaps.synchronizedListMultimap(
				ArrayListMultimap.<Class<?>, Method>create());
		rounds = Multimaps.synchronizedListMultimap(
				ArrayListMultimap.<Class<?>, Method>create());
		intervals = Multimaps.synchronizedListMultimap(
				ArrayListMultimap.<Class<?>, Method>create());
		timeouts = Multimaps.synchronizedListMultimap(
				ArrayListMultimap.<Class<?>, Method>create());
		connects = Multimaps.synchronizedListMultimap(
				ArrayListMultimap.<Class<?>, Method>create());
		disconnects = Multimaps.synchronizedListMultimap(
				ArrayListMultimap.<Class<?>, Method>create());
		
		broadcasts = Multimaps.synchronizedListMultimap(
				ArrayListMultimap.<Class<?>, Method>create());
		services = Multimaps.synchronizedListMultimap(
				ArrayListMultimap.<Class<?>, Method>create());
	}
	
	/**
	 * Add an experiment and process events
	 * @param expId
	 * @param exp
	 */
	boolean processExperiment(String expId, Object exp) {			
		if( exp == null )
			throw new RuntimeException("Refusing to make mappings for a null experiment");
		
		ExperimentServer e = exp.getClass().getAnnotation(ExperimentServer.class);
		if( e == null )
			logger.warn("Class {} does not have @Experiment annotation, but trying mappings anyway", exp.getClass().toString());
		
		if( beanClasses.get(exp.getClass()).size() > 0 ) {
			beans.put(expId, exp);
			beanClasses.put(exp.getClass(), exp);
			return true;
		}				
		
		boolean result = processCallbacks(exp.getClass());
				
		if( result ) {
			beans.put(expId, exp);
			beanClasses.put(exp.getClass(), exp);			
		}
		else {
			throw new IllegalArgumentException("Passed in a class with no callbacks");
		}
		
		return result;
	}

	private boolean processCallbacks(Class<?> klass) {
		boolean result = false;
		 				
		for (Class<?> c = klass; c != Object.class; c = c.getSuperclass())
        {
			Method[] methods = c.getDeclaredMethods();
            for (Method method : methods)
            {
            	result |= processVoid(c, method, 
            			starts, StartExperiment.class);
            	result |= processVoid(c, method, 
            			timeouts, TimeLimit.class);
            	result |= processVoid(c, method,
            			intervals, IntervalEvent.class);
            	
            	result |= processInt(c, method,
            			rounds, StartRound.class);
            	
            	result |= processWorkerActivity(c, method, 
            			connects, WorkerConnect.class);
            	result |= processWorkerActivity(c, method, 
            			disconnects, WorkerDisconnect.class);
            	
            	result |= processBroadcastMessage(c, method,
            			broadcasts, BroadcastMessage.class);
            	result |= processServiceMessage(c, method,
            			services, ServiceMessage.class);
            }
        }
				
		return result;
	}		
	
	/**
	 * Test all the callbacks of a class. This should process the same way as a real experiment.
	 * @param klass
	 * @return
	 */
	public static boolean testCallbacks(Class<?> klass) {
		Multimap<Class<?>, Method> testMap = ArrayListMultimap.<Class<?>, Method>create();
		boolean result = false;
			
		for (Class<?> c = klass; c != Object.class; c = c.getSuperclass())
        {
			Method[] methods = c.getDeclaredMethods();
            for (Method method : methods)
            {
            	result |= processVoid(c, method, 
            			testMap, StartExperiment.class);
            	result |= processVoid(c, method, 
            			testMap, TimeLimit.class);
            	result |= processVoid(c, method,
            			testMap, IntervalEvent.class);
            	
            	result |= processInt(c, method,
            			testMap, StartRound.class);
            	
            	result |= processWorkerActivity(c, method, 
            			testMap, WorkerConnect.class);
            	result |= processWorkerActivity(c, method, 
            			testMap, WorkerDisconnect.class);
            	
            	result |= processBroadcastMessage(c, method,
            			testMap, BroadcastMessage.class);
            	result |= processServiceMessage(c, method,
            			testMap, ServiceMessage.class);
            }
        }
				
		return result;
	}
			
	private static boolean processVoid(Class<?> klass, Method method, 
			Multimap<Class<?>, Method> map, Class<? extends Annotation> annot) {				
		if( method.getAnnotation(annot) == null ) return false;
		
        if (method.getReturnType() != Void.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have void return type");
        if (method.getParameterTypes().length > 0)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have no parameters");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
		
        map.put(klass, method);
		
		return true;
	}

	private static boolean processInt(Class<?> klass, Method method,
			Multimap<Class<?>, Method> map, Class<? extends Annotation> annot) {
		if( method.getAnnotation(annot) == null ) return false;
		
		if (method.getReturnType() != Void.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have void return type");
		Class<?>[] types = method.getParameterTypes();
        if (types.length != 1 || !int.class.isAssignableFrom(types[0]) )
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must accept an int");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
		
        map.put(klass, method);
        
		return true;
	}

	private static boolean processWorkerActivity(Class<?> klass, Method method,
			Multimap<Class<?>, Method> map, Class<? extends Annotation> annot) {
		if( method.getAnnotation(annot) == null ) return false;
		
		if (method.getReturnType() != Void.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have void return type");
		Class<?>[] types = method.getParameterTypes();
        if (types.length != 1 || !HITWorker.class.isAssignableFrom(types[0]) )
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must accept a HITWorker");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
		
        map.put(klass, method);
        
		return true;
	}

	private static boolean processBroadcastMessage(Class<?> klass, Method method,
			Multimap<Class<?>, Method> map, Class<? extends Annotation> annot) {
		if( method.getAnnotation(annot) == null ) return false;
		
		if (method.getReturnType() != Boolean.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have boolean return type");
		Class<?>[] types = method.getParameterTypes();
        if (types.length != 2 || !HITWorker.class.isAssignableFrom(types[0]) || 
        		!Map.class.isAssignableFrom(types[1]) )
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": " +
            		"it must accept a HITWorker, then a Map<String, Object> message");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
		
        map.put(klass, method);
        
		return true;
	}
	
	private static boolean processServiceMessage(Class<?> klass, Method method,
			Multimap<Class<?>, Method> map, Class<? extends Annotation> annot) {
		if( method.getAnnotation(annot) == null ) return false;
		
		if (method.getReturnType() != Void.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have void return type");
		Class<?>[] types = method.getParameterTypes();
        if (types.length != 2 || !HITWorker.class.isAssignableFrom(types[0]) || 
        		!Map.class.isAssignableFrom(types[1]) )
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": " +
            		"it must accept a HITWorker, then a Map<String, Object> message");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
		
        map.put(klass, method);
        
		return true;
	}
	
	private Object invokeMethod(Object bean, Method m, Object... args) {
		boolean accessible = m.isAccessible();
		try {
			// TODO robust-ify the accessibility issue here
			m.setAccessible(true);
			return m.invoke(bean, args);
		} catch (Exception e) {
			logger.warn("Exception invoking {} on {}, ignoring", m, bean.getClass().toString());
			e.printStackTrace();
			return null;
		} finally {
			m.setAccessible(accessible);
		}
	}

	List<Method> getIntervalEvents(String expId) {
		Object bean = beans.get(expId);
		
		List<Method> l = new LinkedList<>();
		
		synchronized(intervals) {
			l.addAll(intervals.get(bean.getClass()));
		}
		
		return l;
	}

	/**
	 * Delivers a broadcast message to an experiment
	 * @param expId
	 * @param message
	 */
	boolean deliverBroadcastMsg(String expId, HITWorker source, Map<String, Object> message) {
		Object bean = beans.get(expId);
		
		boolean forward = false;
		
		synchronized(broadcasts) {
			for( Method m : broadcasts.get(bean.getClass())) {					
				BroadcastMessage ann = m.getAnnotation(BroadcastMessage.class);
				
				if( ann.key().length > 0 ) {
					if( message == null ) continue;
					String key = ann.key()[0];
					if ( !message.containsKey(key) ) continue;
					if ( ann.value().length > 0 && !message.get(key).equals(ann.value()[0])) continue;
				}
				
				forward |= (Boolean) invokeMethod(bean, m, source, message);
			}	
		}		
				
		return forward;
	}
	
	/**
	 * Delivers a service message to an experiment
	 * @param expId
	 * @param source
	 * @param message
	 * @return 
	 */
	void deliverServiceMsg(String expId, HITWorker source, Map<String, Object> message) {
		Object bean = beans.get(expId);
				
		synchronized(services) {
			for( Method m : services.get(bean.getClass())) {
				ServiceMessage ann = m.getAnnotation(ServiceMessage.class);

				if( ann.key().length > 0 ) {
					String key = ann.key()[0];
					if ( !message.containsKey(key) ) continue;
					if ( ann.value().length > 0 && !message.get(key).equals(ann.value()[0])) continue;
				}			

				invokeMethod(bean, m, source, message);
			}
		}
	}
	
	void triggerStart(String expId) {
		Object bean = beans.get(expId);
		
		synchronized(starts) {
			for( Method m : starts.get(bean.getClass())) {
				invokeMethod(bean, m);
			}
		}
	}
	
	void triggerRound(String expId, int round) {
		Object bean = beans.get(expId);
		
		synchronized(rounds) {
			for( Method m : rounds.get(bean.getClass())) {
				invokeMethod(bean, m, round);
			}
		}
	}
	
	/**
	 * Special method to activate interval events, which are not all the same
	 * @param expId
	 * @param method
	 */
	void triggerInterval(String expId, Method method) {
		Object bean = beans.get(expId);
		
		invokeMethod(bean, method);		
	}

	void triggerWorkerConnect(String expId, HITWorkerImpl source) {
		Object bean = beans.get(expId);
		
		synchronized(connects) {
			for( Method m : connects.get(bean.getClass())) {
				invokeMethod(bean, m, source);
			}
		}
	}
	
	void triggerWorkerDisconnect(String expId, HITWorkerImpl source) {
		Object bean = beans.get(expId);
		
		synchronized(disconnects) {
			for( Method m : disconnects.get(bean.getClass())) {
				invokeMethod(bean, m, source);
			}		
		}
	}
	
	void triggerTimelimit(String expId) {
		Object bean = beans.get(expId);
		
		synchronized(timeouts) {
			for( Method m : timeouts.get(bean.getClass())) {
				invokeMethod(bean, m);
			}
		}
	}
	
	/**
	 * Remove callback tracking for an experimentId
	 * @param experimentId
	 */
	void deprocessExperiment(String experimentId) {
		// No null experiments should have been mapped anyway
		Object bean;
		if( (bean = beans.remove(experimentId)) == null ) return;		
		beanClasses.remove(bean.getClass(), bean);
		
		if( beanClasses.get(bean.getClass()).size() > 0 ) return;
				
		// no more beans of this type, de-register callbacks
		for (Class<?> c = bean.getClass(); c != Object.class; c = c.getSuperclass())
		{			
			starts.removeAll(c);
			timeouts.removeAll(c);

			rounds.removeAll(c);

			connects.removeAll(c);
			disconnects.removeAll(c);

			broadcasts.removeAll(c);
			services.removeAll(c);	
		}											
	}			
	
}
