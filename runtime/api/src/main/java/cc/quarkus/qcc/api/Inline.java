package cc.quarkus.qcc.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inline the annotated method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Inline {
    /* TODO: conditions
    Class<? extends InlineCondition>[] when() default {};

    Class<? extends InlineCondition>[] unless() default {};
     */
}
