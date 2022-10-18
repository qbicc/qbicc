package org.qbicc.machine.vfs;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

/**
 * A node backed by a host JVM byte[]
 */
final class HostBytesNode extends Node implements Closeable {
    private byte[] bytes;
    private volatile int mode;
    final long createTime;

    HostBytesNode(final VirtualFileSystem vfs, final byte[] bytes) throws IOException {
        this.bytes = bytes;
        this.createTime = System.currentTimeMillis();
        vfs.addCloseable(this);
        //noinspection OctalInteger
        this.mode = 0444;
    }

    VirtualFileStatBuffer statExisting() throws IOException {
        int ba = VFSUtils.BA_EXISTS;
        ba |= VFSUtils.BA_REGULAR;
        return new VirtualFileStatBuffer(createTime, createTime, createTime, ba, bytes.length, getNodeId());
    }

    @Override
    int openExisting(int fd, VirtualFileSystem vfs, DirectoryNode parent, int flags) throws IOException {
        addLink();
        try {
            if (fd == -1) {
                return vfs.getVioSystem().openInputStream(() -> new ByteArrayInputStream(bytes));
            } else {
                vfs.getVioSystem().openInputStream(fd, () -> new ByteArrayInputStream(bytes));
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
        bytes = new byte[0];
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
