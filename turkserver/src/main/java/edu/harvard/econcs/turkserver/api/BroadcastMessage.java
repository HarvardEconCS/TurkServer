package edu.harvard.econcs.turkserver.api;

import java.lang.annotation.*;

/**
 * Broadcast message. Keys and values define the messages that should be sent
 * to the annotated method. 
 * 
 * usage: boolean getSomeBroadcast(HITWorker worker, Map<String, Object> data)
 * 
 * The method should return true if the message should be broadcast,
 * or false to stop its transmission.
 * 
 * @author mao
 *
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BroadcastMessage {
	String[] key() default {};
	String[] value() default {};
}
