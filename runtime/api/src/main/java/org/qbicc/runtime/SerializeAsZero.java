package org.qbicc.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to indicate that when an instance or static field is
 * serialized to the qbicc initial runtime heap that the value that
 * was present in the field during build time initialization should
 * be replaced by the zero-initializer (0, false, null) appropriate
 * to the type of the field.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface SerializeAsZero {
}


