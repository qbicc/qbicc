package cc.quarkus.qcc.plugin.patcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify a class or element to patch from a patch class, by name.  A patch class or element can be used
 * interchangeably with the original (patched) class or element.
 * <p>
 * A patch class must either extend {@link Object}, the supertype of the patched class, or a patch class of the
 * supertype of the patched class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD })
public @interface Patch {
    /**
     * Specify the class or element name to patch.
     *
     * @return the class or element name
     */
    String value();
}
