package org.qbicc.machine.vfs;

public final class VirtualFileStatBuffer {
    private final long modTime;
    private final long accessTime;
    private final long createTime;
    private final int booleanAttrs;
    private final long size;
    private final long id;

    VirtualFileStatBuffer(long modTime, long accessTime, long createTime, int booleanAttrs, long size, long id) {
        this.modTime = modTime;
        this.accessTime = accessTime;
        this.createTime = createTime;
        this.booleanAttrs = booleanAttrs;
        this.size = size;
        this.id = id;
    }

    public long getModTime() {
        return modTime;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    public int getBooleanAttributes() {
        return booleanAttrs;
    }

    public long getSize() {
        return size;
    }

    public long getFileId() {
        return id;
    }
}
