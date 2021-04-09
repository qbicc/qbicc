package org.qbicc.machine.object;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.qbicc.machine.arch.ObjectType;
import io.smallrye.common.constraint.Assert;

/**
 * An object file format provider.
 */
public interface ObjectFileProvider {
    /**
     * Open an object file on the filesystem for introspection.
     *
     * @param path the path to the object file (must not be {@code null})
     * @return the object file handle
     * @throws IOException if the file open failed for some reason
     */
    ObjectFile openObjectFile(Path path) throws IOException;

    /**
     * Get the object file format.
     *
     * @return the object file format
     */
    ObjectType getObjectType();

    /**
     * Find a provider for the given object type.
     *
     * @param objectType the object file type (must not be {@code null})
     * @param searchLoader the search class loader (must not be {@code null})
     * @return the provider, if any
     */
    static Optional<ObjectFileProvider> findProvider(ObjectType objectType, ClassLoader searchLoader) {
        Assert.checkNotNullParam("objectType", objectType);
        Assert.checkNotNullParam("searchLoader", searchLoader);
        final ServiceLoader<ObjectFileProvider> loader = ServiceLoader.load(ObjectFileProvider.class, searchLoader);
        final Iterator<ObjectFileProvider> iterator = loader.iterator();
        for (;;) try {
            if (! iterator.hasNext()) {
                return Optional.empty();
            }
            ObjectFileProvider next = iterator.next();
            if (next.getObjectType() == objectType) {
                return Optional.of(next);
            }
        } catch (ServiceConfigurationError | RuntimeException ignored) {
            ignored.printStackTrace();
        }
    }
}
