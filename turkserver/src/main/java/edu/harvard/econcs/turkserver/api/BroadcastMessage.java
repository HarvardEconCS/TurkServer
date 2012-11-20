package edu.harvard.econcs.turkserver.api;

import java.lang.annotation.*;

/**
 * Broadcast message. Keys and values define the messages that should be sent
 * to the annotated method. 
 *  
 * @author mao
 *
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BroadcastMessage {
	String[] keys() default {};
	String[] values() default {};
}
