package cc.quarkus.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that the annotated method <em>must</em> be implemented by an intrinsic, as opposed to a {@code native}
 * method implementation.  Compilation fails if a method with this annotation is reachable but no intrinsic has been
 * registered for it.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Intrinsic {
}
