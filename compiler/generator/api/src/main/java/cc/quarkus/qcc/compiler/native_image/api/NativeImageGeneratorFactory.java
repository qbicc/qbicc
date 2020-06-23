package cc.quarkus.qcc.compiler.native_image.api;

import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import io.smallrye.common.constraint.Assert;

/**
 * A back end for the compiler, which consumes a compiled universe and emits something.
 */
public interface NativeImageGeneratorFactory {
    /**
     * The name of the back end.  The name is used to match the requested back end with the actual instance.
     *
     * @return the name (must not be {@code null})
     */
    String getName();

    /**
     * Create a new native image generator.
     *
     * @return the new native image generator
     */
    NativeImageGenerator createGenerator();

    /**
     * Find the named back end.
     *
     * @param name the name (must not be {@code null})
     * @param classLoader the class loader (must not be {@code null})
     * @return the back end (not {@code null})
     */
    static NativeImageGeneratorFactory getInstance(String name, ClassLoader classLoader) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("classLoader", classLoader);
        final ServiceLoader<NativeImageGeneratorFactory> serviceLoader = ServiceLoader.load(NativeImageGeneratorFactory.class, classLoader);
        final Iterator<NativeImageGeneratorFactory> iterator = serviceLoader.iterator();
        for (;;) {
            try {
                if (! iterator.hasNext()) {
                    break;
                }
                final NativeImageGeneratorFactory backEnd = iterator.next();
                if (Objects.equals(backEnd.getName(), name)) {
                    return backEnd;
                }
            } catch (ServiceConfigurationError ignored) {}
        }
        throw new IllegalStateException("No back end found with name `" + name + "`");
    }
}
