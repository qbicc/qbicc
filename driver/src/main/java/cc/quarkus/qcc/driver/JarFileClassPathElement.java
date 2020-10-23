package cc.quarkus.qcc.driver;

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

    String getName() {
        return jarFile.getName();
    }

    ClassPathElement.Resource getResource(final String name) {
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

        ByteBuffer getBuffer() throws IOException {
            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                return ByteBuffer.wrap(inputStream.readAllBytes());
            }
        }

        public void close() {
            // no operation
        }
    }
}
