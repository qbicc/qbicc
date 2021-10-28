package org.qbicc.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that a method does not throw any exception - not even {@link StackOverflowError} or {@link OutOfMemoryError}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface NoThrow {
}
