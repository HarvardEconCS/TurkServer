package edu.harvard.econcs.turkserver.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import edu.harvard.econcs.turkserver.QuizMaterials;
import edu.harvard.econcs.turkserver.api.BroadcastMessage;
import edu.harvard.econcs.turkserver.api.ClientController;
import edu.harvard.econcs.turkserver.api.ClientError;
import edu.harvard.econcs.turkserver.api.ExperimentClient;
import edu.harvard.econcs.turkserver.api.ServiceMessage;
import edu.harvard.econcs.turkserver.api.StartExperiment;
import edu.harvard.econcs.turkserver.api.StartRound;
import edu.harvard.econcs.turkserver.api.TimeLimit;

public class ClientAnnotationManager<C> {
	
	final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
		
	C clientBean;
	
	private Method startExperiment;
	private Method startRound;
	private Method timeLimit;
	private Method clientError;
	
	private List<Method> broadcasts;
	private List<Method> services;

	public ClientAnnotationManager(ClientController client, Class<C> clientClass) throws Exception {		
		Constructor<C> cons = null;
		
		try {			
			// TODO allow lobbyController as well
			cons = clientClass.getConstructor(ClientController.class);
		} catch (NoSuchMethodException e) {
			logger.severe("Error: " + clientClass + " must have a public, one-argument constructor that accepts a ClientController\n");
			throw e;
		}		
		
		ExperimentClient e = clientClass.getAnnotation(ExperimentClient.class);
		if( e == null )
			logger.warning("Class " + clientClass.toString() +
					" does not have @ExperimentClient annotation, but trying callbacks anyway");
						
		broadcasts = new LinkedList<Method>();
		services = new LinkedList<Method>();
		
		boolean processed = false;
		for (Class<?> c = clientClass; c != Object.class; c = c.getSuperclass())
        {
			Method[] methods = c.getDeclaredMethods();
            for (Method method : methods)
            {
            	processed |= processStartExperiment(method);
            	processed |= processStartRound(method);
            	processed |= processTimeLimit(method);
            	processed |= processClientError(method);
            	
            	processed |= processBroadcast(method);
            	processed |= processService(method);
            }
        }
		
		if( !processed )
			logger.warning("Didn't find any methods in " + clientClass.toString());
		
		clientBean = cons.newInstance(client);
	}

	public C getClientBean() {		
		return clientBean;
	}

	private boolean processStartExperiment(Method method) {
		Class<? extends Annotation> annot = StartExperiment.class;
		if( method.getAnnotation(annot) == null ) return false;
		
        if (method.getReturnType() != Void.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have void return type");
        if (method.getParameterTypes().length > 0)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have no parameters");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
		
		if( startExperiment != null ) {
			logger.warning("Already found a " + annot.toString() + " method, ignoring " + method.toString());
			return false;
		}			
		
		startExperiment = method;		
		return true;
	}

	private boolean processStartRound(Method method) {
		Class<? extends Annotation> annot = StartRound.class;
		if( method.getAnnotation(annot) == null ) return false;
		
		if (method.getReturnType() != Void.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have void return type");
		Class<?>[] types = method.getParameterTypes();
        if (types.length != 1 || !int.class.isAssignableFrom(types[0]) )
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must accept an int");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
		
		if( startRound != null ) {
			logger.warning("Already found a " + annot.toString() + " method, ignoring " + method.toString());
			return false;
		}
				
		startRound = method;
		return true;
	}

	private boolean processTimeLimit(Method method) {
		Class<? extends Annotation> annot = TimeLimit.class;
		if( method.getAnnotation(annot) == null ) return false;
		
        if (method.getReturnType() != Void.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have void return type");
        if (method.getParameterTypes().length > 0)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have no parameters");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
		
		if( timeLimit != null ) {
			logger.warning("Already found a " + annot.toString() + " method, ignoring " + method.toString());
			return false;
		}	
		
		timeLimit = method;
		return true;
	}

	private boolean processClientError(Method method) {
		Class<? extends Annotation> annot = ClientError.class;
		if( method.getAnnotation(annot) == null ) return false;
		
		if (method.getReturnType() != Void.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have void return type");
		Class<?>[] types = method.getParameterTypes();
        if (types.length != 1 || !String.class.isAssignableFrom(types[0]) )
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must accept a string");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
		
		if( clientError != null ) {
			logger.warning("Already found a " + annot.toString() + " method, ignoring " + method.toString());
			return false;
		}
				
		clientError = method;
		return true;
	}

	private boolean processBroadcast(Method method) {
		Class<? extends Annotation> annot = BroadcastMessage.class;
		if( method.getAnnotation(annot) == null ) return false;
		
		if (method.getReturnType() != Void.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have void return type");
		Class<?>[] types = method.getParameterTypes();
        if (types.length != 1 || !Map.class.isAssignableFrom(types[0]) )
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must accept a Map<String, Object>");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
		
		broadcasts.add(method);
		return true;
	}

	private boolean processService(Method method) {
		Class<? extends Annotation> annot = ServiceMessage.class;
		if( method.getAnnotation(annot) == null ) return false;
		
		if (method.getReturnType() != Void.TYPE)
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must have void return type");
		Class<?>[] types = method.getParameterTypes();
        if (types.length != 1 || !Map.class.isAssignableFrom(types[0]) )
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must accept a Map<String, Object>");
        if (Modifier.isStatic(method.getModifiers()))
            throw new RuntimeException("Invalid " + annot.toString() + " method " + method + ": it must not be static");
        
        services.add(method);
		return true;
	}

	private Object invokeMethod(Method m, Object... args) {
		boolean accessible = m.isAccessible();
		try {
			// TODO robust-ify the accessibility issue here
			m.setAccessible(true);
			return m.invoke(clientBean, args);
		} catch (Exception e) {
			logger.warning("Exception invoking " + m + " on " + clientBean.getClass().toString() + ", ignoring");
			e.printStackTrace();
			return null;
		} finally {
			m.setAccessible(accessible);
		}		
	}

	public void triggerQuiz(QuizMaterials qm) {
		// TODO Auto-generated method stub
		
	}

	public void triggerRequestUsername() {
		// TODO Auto-generated method stub
		
	}

	public void triggerJoinLobby() {
		// TODO Auto-generated method stub
		
	}

	public void triggerUpdateLobby(Map<String, Object> data) {
		// TODO Auto-generated method stub
		
	}

	public void triggerStartExperiment() {
		if( startExperiment != null ) invokeMethod(startExperiment);
	}
	
	public void triggerStartRound(int n) {
		if( startRound != null ) invokeMethod(startRound, n);
	}
	
	public void triggerFinishExperiment() {
		// TODO Auto-generated method stub
		
	}

	public void triggerTimeLimit() {
		if( timeLimit != null ) invokeMethod(timeLimit);
	}
	
	public void triggerClientError(String msg) {
		if( clientError != null ) invokeMethod(clientError, msg);
	}
	
	public void deliverBroadcast(Map<String, Object> message) {
		for( Method m : broadcasts ) {
			BroadcastMessage ann = m.getAnnotation(BroadcastMessage.class);
			
			if( ann.key().length > 0 ) {
				String key = ann.key()[0];
				if (!message.containsKey(key) ) continue;
				if ( ann.value().length > 0 && !message.get(key).equals(ann.value()[0])) continue;
			}			
			
			invokeMethod(m, message);
		}
	}
	
	public void deliverService(Map<String, Object> message) {		
		for( Method m : services ) {
			ServiceMessage ann = m.getAnnotation(ServiceMessage.class);
			
			if( ann.key().length > 0 ) {
				String key = ann.key()[0];
				if (!message.containsKey(key) ) continue;
				if ( ann.value().length > 0 && !message.get(key).equals(ann.value()[0])) continue;
			}			
			
			invokeMethod(m, message);
		}
	}
}
