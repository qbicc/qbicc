package org.qbicc.runtime.patcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BooleanSupplier;

/**
 * When applied to a method or constructor, specify that the annotated element is a replacement for the corresponding
 * original method or constructor. The signature must be equivalent to the original method or constructor (after type
 * aliasing has taken place).  The replacement body is used instead of the original.
 * <p>
 * When applied to a field, specify that the annotated element's initialization should be replaced with the
 * initialization expression of the annotated patch field.  Note any side-effects of the original field initialization
 * will be retained unless all of the original initialization is patched or removed; this is because such side-effects
 * could affect other aspects of the original initialization, and so to remove them would cause hard-to-understand
 * behavior changes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD })
@Repeatable(Replace.List.class)
public @interface Replace {
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
        Replace[] value();
    }
}
