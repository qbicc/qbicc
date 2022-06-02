package org.qbicc.runtime.host;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public final class HostBasicFileAttributes implements BasicFileAttributes {
    static final int BA_EXISTS    = 0x01;
    static final int BA_REGULAR   = 0x02;
    static final int BA_DIRECTORY = 0x04;
    static final int BA_HIDDEN    = 0x08;

    private final long modTime;
    private final long accessTime;
    private final long createTime;
    private final boolean regularFile;
    private final boolean directory;
    private final boolean symlink;
    private final boolean other;
    private final long size;
    private final long inode;

    HostBasicFileAttributes(long[] data) {
        modTime = data[0];
        accessTime = data[1];
        createTime = data[2];
        long attrs = data[3];
        regularFile = (attrs & BA_REGULAR) != 0;
        directory = (attrs & BA_DIRECTORY) != 0;
        symlink = false; // todo
        other = (attrs & (BA_REGULAR | BA_DIRECTORY)) == 0 && (attrs & BA_EXISTS) != 0;
        size = data[4];
        inode = data[5];
    }

    @Override
    public FileTime lastModifiedTime() {
        return FileTime.fromMillis(modTime);
    }

    @Override
    public FileTime lastAccessTime() {
        return FileTime.fromMillis(accessTime);
    }

    @Override
    public FileTime creationTime() {
        return FileTime.fromMillis(createTime);
    }

    @Override
    public boolean isRegularFile() {
        return regularFile;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    @Override
    public boolean isSymbolicLink() {
        return symlink;
    }

    @Override
    public boolean isOther() {
        return other;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public Object fileKey() {
        return Long.valueOf(inode);
    }
}
