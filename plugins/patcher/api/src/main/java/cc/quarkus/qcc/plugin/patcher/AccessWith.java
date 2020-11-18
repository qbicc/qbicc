package cc.quarkus.qcc.plugin.patcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BooleanSupplier;

/**
 * Specify that a field must be accessed with the given value. If multiple are given, the first accessor containing
 * a matching rule is used.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AccessWith.List.class)
public @interface AccessWith {
    /**
     * The accessor implementation.
     *
     * @return the accessor implementation class
     */
    Class<? extends Accessor<?>> value();

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
        AccessWith[] value();
    }
}
