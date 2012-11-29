package edu.harvard.econcs.turkserver.api;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceMessage {
	String[] key() default {};
	String[] value() default {};
}
