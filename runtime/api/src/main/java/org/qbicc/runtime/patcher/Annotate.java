package org.qbicc.runtime.patcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BooleanSupplier;

/**
 * Copy the annotations from this member to the patched member.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD })
@Repeatable(Annotate.List.class)
public @interface Annotate {
    /**
     * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
     *
     * @return the condition classes
     */
    Class<? extends BooleanSupplier>[] when() default {};

    /**
     * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
     *
     * @return the condition classes
     */
    Class<? extends BooleanSupplier>[] unless() default {};

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        Annotate[] value();
    }
}
