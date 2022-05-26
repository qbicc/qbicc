package org.qbicc.machine.vfs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;

/**
 * A real file node backed by the host filesystem.
 */
final class FileNode extends Node implements Closeable {
    private final Path tempFilePath;
    private volatile int mode;

    FileNode(final VirtualFileSystem vfs, final int mode) throws IOException {
        tempFilePath = Files.createTempFile("qbicc-vfs", "", PosixFilePermissions.asFileAttribute(EnumSet.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE
        )));
        vfs.addCloseable(this);
        //noinspection OctalInteger
        this.mode = mode & 0777;
    }

    @Override
    int openExisting(int fd, VirtualFileSystem vfs, DirectoryNode parent, int flags) throws IOException {
        EnumSet<StandardOpenOption> options = getOptions(flags);
        addLink();
        try {
            if (fd == -1) {
                return vfs.getVioSystem().openRealFile(tempFilePath, options, Set.of(), this::removeLink);
            } else {
                vfs.getVioSystem().openRealFile(fd, tempFilePath, options, Set.of(), this::removeLink);
                return fd;
            }
        } catch (Throwable t) {
            try {
                removeLink();
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }
            throw t;
        }
    }

    private EnumSet<StandardOpenOption> getOptions(final int flags) throws NotDirectoryException {
        EnumSet<StandardOpenOption> set = EnumSet.noneOf(StandardOpenOption.class);
        int accessMode = flags & VFSUtils.O_ACCESS_MODE_MASK;
        if (accessMode == VFSUtils.O_RDONLY || accessMode == VFSUtils.O_RDWR) {
            set.add(StandardOpenOption.READ);
        }
        if (accessMode == VFSUtils.O_WRONLY || accessMode == VFSUtils.O_RDWR) {
            set.add(StandardOpenOption.WRITE);
        }
        if ((flags & VFSUtils.O_TRUNC) != 0) {
            set.add(StandardOpenOption.TRUNCATE_EXISTING);
        }
        if ((flags & VFSUtils.O_DIRECTORY) != 0) {
            throw new NotDirectoryException("Not a directory");
        }
        return set;
    }

    @Override
    public void close() throws IOException {
        Files.delete(tempFilePath);
    }

    @Override
    int getMode() {
        return mode;
    }

    @Override
    void changeMode(int newMode) {
        //noinspection OctalInteger
        this.mode = newMode & 0777;
    }
}
