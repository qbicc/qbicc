package org.qbicc.runtime.patcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify that the annotated {@code static} method or constructor patches the corresponding initializer of the original
 * class.  When applied to a {@code static} method annotated with {@link Remove} or {@link Replace}, all of the original
 * static initialization of the patched class is removed; otherwise, additional initialization is added.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface PatchInitializer {
}
