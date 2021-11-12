package org.qbicc.runtime.patcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that the annotated element is a run time aspect for the patched class.  A class can have any number
 * of run time aspects, however no two run time aspects may define the same members other than the static initializer
 * itself.  This annotation will cause initializers and fields defined on the class to be run time only, but it does
 * not affect methods defined on the class.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface RunTimeAspect {
}
