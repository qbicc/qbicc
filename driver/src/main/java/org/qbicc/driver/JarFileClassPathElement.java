package org.qbicc.driver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class JarFileClassPathElement extends ClassPathElement {
    private final JarFile jarFile;

    JarFileClassPathElement(final JarFile jarFile) {
        this.jarFile = jarFile;
    }

    public String getName() {
        return jarFile.getName();
    }

    public ClassPathElement.Resource getResource(final String name) {
        JarEntry jarEntry = jarFile.getJarEntry(name);
        return jarEntry == null ? NON_EXISTENT : new Resource(jarEntry);
    }

    public void close() throws IOException {
        jarFile.close();
    }

    final class Resource extends ClassPathElement.Resource {
        private final JarEntry entry;

        Resource(final JarEntry entry) {
            this.entry = entry;
        }

        public ByteBuffer getBuffer() throws IOException {
            try (InputStream inputStream = openStream()) {
                return ByteBuffer.wrap(inputStream.readAllBytes());
            }
        }

        @Override
        public InputStream openStream() throws IOException {
            return jarFile.getInputStream(entry);
        }

        public void close() {
            // no operation
        }
    }
}
