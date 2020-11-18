package cc.quarkus.qcc.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated method is detached from the VM, and thus may not use any Java VM facilities such as heap allocation/GC,
 * safepoints, {@link Thread#currentThread()}, exceptions, etc.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Detached {
}
