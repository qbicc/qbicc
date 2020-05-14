package cc.quarkus.qcc.compiler.backend.api;

import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import cc.quarkus.qcc.type.universe.Universe;
import io.smallrye.common.constraint.Assert;

/**
 * A back end for the compiler, which consumes a compiled universe and emits something.
 */
public interface BackEnd {
    /**
     * The name of the back end.  The name is used to match the requested back end with the actual instance.
     *
     * @return the name (must not be {@code null})
     */
    String getName();

    /**
     * Produce something from a compiled universe.
     *
     * @param universe the compiled universe (must not be {@code null})
     */
    void compile(Universe universe);

    /**
     * Find the named back end.
     *
     * @param name the name (must not be {@code null})
     * @param classLoader the class loader (must not be {@code null})
     * @return the back end (not {@code null})
     */
    static BackEnd getInstance(String name, ClassLoader classLoader) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("classLoader", classLoader);
        final ServiceLoader<BackEnd> serviceLoader = ServiceLoader.load(BackEnd.class, classLoader);
        final Iterator<BackEnd> iterator = serviceLoader.iterator();
        for (;;) {
            try {
                if (! iterator.hasNext()) {
                    break;
                }
                final BackEnd backEnd = iterator.next();
                if (Objects.equals(backEnd.getName(), name)) {
                    return backEnd;
                }
            } catch (ServiceConfigurationError ignored) {}
        }
        throw new IllegalStateException("No back end found with name `" + name + "`");
    }
}
