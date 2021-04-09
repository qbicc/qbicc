package org.qbicc.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that instances of this class must be pinned in memory (i.e. its address cannot change and can always be taken).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Pinned {
}
