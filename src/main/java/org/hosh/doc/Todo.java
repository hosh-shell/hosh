package org.hosh.doc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Document a planned feature or improvement.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({
		ElementType.TYPE, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE, ElementType.FIELD
})
public @interface Todo {
	String description();
}
