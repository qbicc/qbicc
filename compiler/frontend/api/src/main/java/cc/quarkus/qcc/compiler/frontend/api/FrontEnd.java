package cc.quarkus.qcc.compiler.frontend.api;

import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import cc.quarkus.qcc.type.universe.Universe;
import io.smallrye.common.constraint.Assert;

/**
 * A front end for compilation.
 */
public interface FrontEnd {
    /**
     * The name of the front end.  The name is used to match the requested front end with the actual instance.
     *
     * @return the name (must not be {@code null})
     */
    String getName();

    /**
     * Produce a compiled universe which can be handed to a back end.  A {@code Context} will be present for
     * the duration of the compilation process.  If errors are reported during front end processing, the back
     * end will not be invoked.
     *
     * @return the compiled universe (must not be {@code null})
     */
    Universe compile();

    /**
     * Find the named front end.
     *
     * @param name the name (must not be {@code null})
     * @param classLoader the class loader (must not be {@code null})
     * @return the front end (not {@code null})
     */
    static FrontEnd getInstance(String name, ClassLoader classLoader) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("classLoader", classLoader);
        final ServiceLoader<FrontEnd> serviceLoader = ServiceLoader.load(FrontEnd.class, classLoader);
        final Iterator<FrontEnd> iterator = serviceLoader.iterator();
        for (;;) {
            try {
                if (! iterator.hasNext()) {
                    break;
                }
                final FrontEnd frontEnd = iterator.next();
                if (Objects.equals(frontEnd.getName(), name)) {
                    return frontEnd;
                }
            } catch (ServiceConfigurationError ignored) {}
        }
        throw new IllegalStateException("No front end found with name `" + name + "`");
    }
}
