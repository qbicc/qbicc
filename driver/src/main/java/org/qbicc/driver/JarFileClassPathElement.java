package org.qbicc.driver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.qbicc.machine.vfs.VirtualFileSystem;
import org.qbicc.machine.vfs.VirtualPath;

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

    @Override
    public void mount(VirtualFileSystem vfs, VirtualPath mountPoint) throws IOException {
        Iterator<JarEntry> iterator = jarFile.entries().asIterator();
        while (iterator.hasNext()) {
            JarEntry ze = iterator.next();
            VirtualPath vp = mountPoint.resolve(ze.getRealName());
            if (ze.isDirectory()) {
                //noinspection OctalInteger
                vfs.mkdirs(vp, 0755);
            } else {
                VirtualPath parent = vp.getParent();
                //noinspection OctalInteger
                vfs.mkdirs(parent, 0755);
                vfs.bindZipEntry(vp, jarFile, ze, true);
            }
        }
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
