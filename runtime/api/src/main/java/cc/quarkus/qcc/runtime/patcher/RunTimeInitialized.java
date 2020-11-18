package cc.quarkus.qcc.runtime.patcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BooleanSupplier;

/**
 * Specify that the annotated class or field should be initialized at run time.  Any build-time reference to a run-time
 * initialized field or class that would normally require the field or class to be initialized will result in a
 * compilation error.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
@Repeatable(RunTimeInitialized.List.class)
public @interface RunTimeInitialized {
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
        RunTimeInitialized[] value();
    }
}
