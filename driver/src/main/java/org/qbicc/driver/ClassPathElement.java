package org.qbicc.driver;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.jar.JarFile;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.definition.ByteBufferInputStream;

/**
 * A class path element that the driver can consume.  Class path elements must be closed.
 */
public abstract class ClassPathElement implements Closeable {
    ClassPathElement() {}

    /**
     * Get the name of this element.  The name is used for logging purposes and should normally point to the path
     * of the element.
     *
     * @return the name of this element (not {@code null})
     */
    public abstract String getName();

    /**
     * Get a closeable resource from this element.  If there is no matching element then {@link #NON_EXISTENT} will
     * be returned.
     *
     * @param name the path name (must not be {@code null})
     * @return the resource (not {@code null})
     * @throws IOException if reading the resource failed
     */
    public abstract Resource getResource(String name) throws IOException;

    /**
     * Get a class path element for the given directory path.
     *
     * @param path the directory path (must not be {@code null})
     * @return the class path element (not {@code null})
     */
    public static ClassPathElement forDirectory(Path path) {
        Assert.checkNotNullParam("path", path);
        return new DirectoryClassPathElement(path);
    }

    /**
     * Get a class path element for the given JAR file.
     *
     * @param path the path to the JAR file
     * @return the class path element (not {@code null})
     * @throws IOException if the JAR file failed to open
     */
    public static ClassPathElement forJarFile(Path path) throws IOException {
        Assert.checkNotNullParam("path", path);
        return new JarFileClassPathElement(new JarFile(path.toFile()));
    }

    /**
     * Get a class path element for the given JAR file.
     *
     * @param file the path to the JAR file
     * @return the class path element (not {@code null})
     * @throws IOException if the JAR file failed to open
     */
    public static ClassPathElement forJarFile(File file) throws IOException {
        Assert.checkNotNullParam("path", file);
        return forJarFile(file.toPath());
    }

    /**
     * A resource that can be mapped or loaded into RAM.
     */
    public static abstract class Resource implements Closeable {
        Resource() {}

        /**
         * Get a buffer corresponding to this resource. The buffer may be retained after the resource is closed.
         *
         * @return the buffer (not {@code null})
         * @throws IOException if reading the resource failed
         */
        public abstract ByteBuffer getBuffer() throws IOException;

        public InputStream openStream() throws IOException {
            return new ByteBufferInputStream(getBuffer());
        }

        public final BufferedReader openBufferedReader() throws IOException {
            return new BufferedReader(new InputStreamReader(openStream(), StandardCharsets.UTF_8));
        }
    }

    /**
     * A non-existent resource.
     */
    static final Resource NON_EXISTENT = new Resource() {
        static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

        public ByteBuffer getBuffer() {
            return EMPTY;
        }

        @Override
        public InputStream openStream() {
            return InputStream.nullInputStream();
        }

        public void close() {
        }
    };
}
