package org.qbicc.driver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

final class DirectoryClassPathElement extends ClassPathElement {
    private final Path baseDir;

    DirectoryClassPathElement(final Path baseDir) {
        this.baseDir = baseDir;
    }

    String getName() {
        return baseDir.toString();
    }

    ClassPathElement.Resource getResource(final String name) throws IOException {
        Path resourcePath = baseDir.resolve(name);
        return ! Files.exists(resourcePath) ? NON_EXISTENT : new Resource(FileChannel.open(resourcePath, Set.of(StandardOpenOption.READ)));
    }

    public void close() {
        // no operation
    }

    static final class Resource extends ClassPathElement.Resource {
        private final FileChannel channel;
        private ByteBuffer buffer;

        Resource(final FileChannel channel) {
            this.channel = channel;
        }

        ByteBuffer getBuffer() throws IOException {
            // todo: this won't work on windows - we'll have to keep the channel open there and tie it to the element
            ByteBuffer buffer = this.buffer;
            if (buffer == null) {
                buffer = this.buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
            }
            return buffer;
        }

        public void close() throws IOException {
            channel.close();
        }
    }
}
