package org.qbicc.runtime.patcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BooleanSupplier;

/**
 * Indicate that the annotated class's initializer should replace the initializer of the patch class.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Repeatable(ReplaceInit.List.class)
public @interface ReplaceInit {
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

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @interface List {
        ReplaceInit[] value();
    }
}
