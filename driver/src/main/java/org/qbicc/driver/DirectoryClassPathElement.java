package org.qbicc.driver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import io.smallrye.common.os.OS;
import org.qbicc.machine.vfs.VirtualFileSystem;
import org.qbicc.machine.vfs.VirtualPath;

final class DirectoryClassPathElement extends ClassPathElement {
    private final Path baseDir;

    DirectoryClassPathElement(final Path baseDir) {
        this.baseDir = baseDir;
    }

    public String getName() {
        return baseDir.toString();
    }

    public ClassPathElement.Resource getResource(final String name) throws IOException {
        Path resourcePath = baseDir.resolve(name);
        return ! Files.exists(resourcePath) ? NON_EXISTENT : new Resource(FileChannel.open(resourcePath, Set.of(StandardOpenOption.READ)));
    }

    public void close() {
        // no operation
    }

    public void mount(VirtualFileSystem vfs, VirtualPath mountPoint) throws IOException {
        mount(vfs, mountPoint, baseDir);
    }

    private void mount(VirtualFileSystem vfs, VirtualPath mountPoint, Path dirPath) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    VirtualPath subDirPath = mountPoint.resolve(path.getFileName().toString());
                    //noinspection OctalInteger
                    vfs.mkdir(subDirPath, 0755);
                    mount(vfs, subDirPath, path);
                } else {
                    vfs.bindExternalNode(mountPoint, path, true, false);
                }
            }
        }
    }

    static final class Resource extends ClassPathElement.Resource {
        private final FileChannel channel;
        private ByteBuffer buffer;

        Resource(final FileChannel channel) {
            this.channel = channel;
        }

        public ByteBuffer getBuffer() throws IOException {
            ByteBuffer buffer = this.buffer;
            if (buffer == null) {
                if (OS.current() == OS.WINDOWS) {
                    // just read the whole thing; no need to close because the outer close will close the channel
                    buffer = this.buffer = ByteBuffer.wrap(Channels.newInputStream(channel).readAllBytes());
                } else {
                    buffer = this.buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
                }
            }
            return buffer;
        }

        public void close() throws IOException {
            channel.close();
        }
    }
}
