package org.qbicc.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that a method does not return.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface NoReturn {
}
