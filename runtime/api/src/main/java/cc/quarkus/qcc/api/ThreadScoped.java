package cc.quarkus.qcc.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that a {@code static} field has thread-local duration. Such fields are initialized whenever a thread
 * starts. The field <em>must</em> be {@code static}, and <em>may</em> be {@code final}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ThreadScoped {
}
