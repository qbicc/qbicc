package org.qbicc.runtime;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that a method cannot be interrupted by a safepoint poll.
 */
@Target({ METHOD, CONSTRUCTOR })
@Retention(RetentionPolicy.CLASS)
public @interface NoSafePoint {
}
