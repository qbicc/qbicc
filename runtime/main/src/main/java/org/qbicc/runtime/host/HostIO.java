package org.qbicc.runtime.host;

import java.io.IOException;

/**
 * I/O methods that run on the host during build time.
 * These methods are similar to POSIX but throw exceptions instead of using {@code errno}.
 * All file descriptors opened or created during build time are closed at run time.
 */
public final class HostIO {

    private HostIO() {
    }

    public static final int O_ACCESS_MODE_MASK = 0b11;

    public static final int O_RDONLY = 0;
    public static final int O_WRONLY = 1;
    public static final int O_RDWR = 2;

    public static final int O_CREAT = 1 << 2;
    public static final int O_EXCL = 1 << 3;
    public static final int O_TRUNC = 1 << 4;
    public static final int O_APPEND = 1 << 5;

    public static final int O_DIRECTORY = 1 << 6;

    // Filesystem ops

    public static native int open(String pathName, int openFlags, int mode) throws IOException;

    public static native void reopen(int fd, String pathName, int openFlags) throws IOException;

    public static native void mkdir(String pathName, int mode) throws IOException;

    public static native void unlink(String pathName) throws IOException;

    public static native int getBooleanAttributes(String pathName) throws IOException;

    public static native void checkAccess(final String pathName) throws IOException;

    // General I/O ops

    /**
     * Close the given file descriptor.
     * The descriptor is always closed, even if this method throws a final exception.
     *
     * @param fd the file descriptor number
     * @throws IOException if a final close action failed, or if {@code fd} is not a valid file descriptor
     */
    public static native void close(int fd) throws IOException;

    public static native int[] pipe() throws IOException;

    public static native int[] socketpair() throws IOException;

    public static native int dup(int fd) throws IOException;

    public static native void dup2(int oldFd, int newFd) throws IOException;

    public static native int read(int fd, byte[] dest, int off, int len) throws IOException;

    public static native int readSingle(int fd) throws IOException;

    public static native long available(int fd) throws IOException;

    public static native int write(int fd, byte[] src, int off, int len) throws IOException;

    public static native int append(int fd, byte[] src, int off, int len) throws IOException;

    public static native void writeSingle(int fd, int b) throws IOException;

    public static native void appendSingle(int fd, int b) throws IOException;

    public static native boolean isAppend(final int fd);

    public static native void fsync(int fd) throws IOException;

    public static native long getFileSize(int fd) throws IOException;

    public static native long seekRelative(int fd, long offs) throws IOException;

    public static native long seekAbsolute(int fd, long offs) throws IOException;
}
