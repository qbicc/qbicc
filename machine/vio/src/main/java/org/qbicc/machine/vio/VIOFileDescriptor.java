package org.qbicc.machine.vio;

import java.io.IOException;

import io.smallrye.common.function.ExceptionConsumer;

/**
 *
 */
final class VIOFileDescriptor implements Comparable<VIOFileDescriptor> {
    private volatile VIOFile file;
    private final int fdNum;

    VIOFileDescriptor(final int fdNum) {
        this.fdNum = fdNum;
    }

    void run() {}

    public int getFdNum() {
        return fdNum;
    }

    public VIOFile getFile() {
        return file;
    }

    @Override
    public int compareTo(VIOFileDescriptor o) {
        return Integer.compareUnsigned(fdNum, o.fdNum);
    }

    void setFile(final VIOFile file) {
        assert Thread.holdsLock(this);
        this.file = file;
    }

    public VIOFile getAndClearFile() {
        synchronized (this) {
            VIOFile file = this.file;
            this.file = null;
            return file;
        }
    }

    public VIOFile replaceFile(final VIOFile newFile) {
        synchronized (this) {
            VIOFile file = this.file;
            this.file = newFile;
            return file;
        }
    }

    boolean tryOpen(final ExceptionConsumer<VIOFileDescriptor, IOException> action) throws IOException {
        synchronized (this) {
            if (file == null) {
                action.accept(this);
                return true;
            }
        }
        return false;
    }
}
