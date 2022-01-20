package org.qbicc.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to indicate that when an instance or static field
 * of boolean type is serialized to the qbicc initial runtime heap that the value that
 * was present in the field during build time initialization should
 * be replaced by the value provided by the annotation.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface SerializeBooleanAs {
    /** The value to serialize */
    boolean value();
}
