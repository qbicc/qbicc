package org.qbicc.runtime;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method can only be executed at Build Time;
 * the body of an annotated method will be replaced by an unconditional throw
 * of a NotReachableException in the compiled executable.
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface BuildTimeOnly {
}
