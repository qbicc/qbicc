package org.qbicc.runtime.patcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the class being patched by the annotated class.  If the class cannot be represented as a literal, then
 * {@link Patch} must be used instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PatchClass {
    /**
     * Specify the class to patch.
     *
     * @return the class to patch
     */
    Class<?> value();
}
