package org.qbicc.machine.vfs;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotLinkException;
import java.util.concurrent.atomic.AtomicLong;

abstract class Node implements Closeable {
    private static final AtomicLong nodeIdCounter = new AtomicLong(1);
    private static final VarHandle linksHandle = ConstantBootstraps.fieldVarHandle(
        MethodHandles.lookup(),
        "links",
        VarHandle.class,
        Node.class,
        int.class
    );
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int links = 1;
    private final long nodeId = nodeIdCounter.getAndIncrement();

    Node() {
    }

    final void addLink() throws NoSuchFileException {
        int links;
        do {
            links = this.links;
            if (links == 0) {
                throw new NoSuchFileException("<deleted>");
            }
        } while (!linksHandle.compareAndSet(this, links, links + 1));
    }

    final void removeLink() throws IOException {
        int links;
        do {
            links = this.links;
            if (links == 0) {
                throw new IllegalStateException("Link reference count mismatch");
            }
        } while (!linksHandle.compareAndSet(this, links, links - 1));
        if (links == 1) {
            close();
        }
    }

    abstract int openExisting(int fd, VirtualFileSystem vfs, DirectoryNode parent, int flags) throws IOException;

    VirtualPath readLink(VirtualFileSystem fileSystem, VirtualPath vp, int vpi) throws IOException {
        throw new NotLinkException("Cannot open non-link node");
    }

    long getNodeId() {
        return nodeId;
    }

    abstract int getMode();

    abstract void changeMode(int newMode) throws AccessDeniedException;

    boolean isReadable() {
        //noinspection OctalInteger
        return (getMode() & 0444) != 0;
    }

    boolean isWritable() {
        //noinspection OctalInteger
        return (getMode() & 0222) != 0;
    }

    boolean isExecutable() {
        //noinspection OctalInteger
        return (getMode() & 0111) != 0;
    }

    void checkMode(final String name, final int accessMode) throws AccessDeniedException {
        int mode = getMode();
        //noinspection OctalInteger
        if ((accessMode == VFSUtils.O_RDONLY || accessMode == VFSUtils.O_RDWR) && (mode & 0444) == 0
            || (accessMode == VFSUtils.O_WRONLY || accessMode == VFSUtils.O_RDWR) && (mode & 0222) == 0) {
            throw new AccessDeniedException(name);
        }
    }

    @Override
    public void close() throws IOException {
    }
}
