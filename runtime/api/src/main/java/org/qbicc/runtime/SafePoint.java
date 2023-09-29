package org.qbicc.runtime;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Describe the safepoint behavior for a method.
 * Regular methods will have the behavior of {@link SafePointBehavior#POLLING} if this annotation is not given.
 * If given, the behavior will default to {@link SafePointBehavior#ENTER} unless specifically overridden.
 * <p>
 * Additional thread-state flags may be given to set or clear on safepoint entry or exit.
 * These values will be ignored for calls which do not cause safepoint entry.
 * The values are only meaningful to the runtime library.
 * <p>
 * Taking a pointer to a method will discard/obscure its safepoint mode.
 * External callers to functions will not cause safepoint to be entered; it is assumed that such callers
 * must already have been in a safepoint.
 */
@Target({METHOD, CONSTRUCTOR})
@Retention(CLASS)
public @interface SafePoint {
    SafePointBehavior value() default SafePointBehavior.ENTER;

    int setOnEntry() default 0;

    int clearOnEntry() default 0;

    int setOnExit() default 0;

    int clearOnExit() default 0;
}
