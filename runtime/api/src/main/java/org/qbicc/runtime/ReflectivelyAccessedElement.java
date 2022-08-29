package org.qbicc.runtime;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used in @ReflectivelyAccesses annotations to list the fields and methods
 * of another class that the annotated class might access reflectively.
 */
@Retention(RetentionPolicy.CLASS)
public @interface ReflectivelyAccessedElement {
    Class<?> clazz();
    String method() default "";
    String field() default "";
    Class<?>[] params() default {};
}
