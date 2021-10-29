package org.qbicc.runtime;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inline the annotated method.  Note that only monomorphic method call sites will be considered for inlining.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface Inline {
    /**
     * Get the inlining condition.
     *
     * @return the inlining condition
     */
    InlineCondition value() default InlineCondition.HINT;
}
