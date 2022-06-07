package org.qbicc.machine.vfs;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 */
final class ZipEntryNode extends Node {
    private final ZipFile zf;
    private final ZipEntry ze;
    private final long defaultTime = System.currentTimeMillis();

    ZipEntryNode(ZipFile zf, ZipEntry ze) {
        this.zf = zf;
        this.ze = ze;
    }

    @Override
    VirtualFileStatBuffer statExisting() {
        int attr = VFSUtils.BA_EXISTS;
        FileTime ctime = ze.getCreationTime();
        FileTime mtime = ze.getLastModifiedTime();
        if (ctime == null) {
            if (mtime == null) {
                ctime = mtime = FileTime.fromMillis(defaultTime);
            } else {
                ctime = mtime;
            }
        } else if (mtime == null) {
            mtime = ctime;
        }
        FileTime atime = ze.getLastAccessTime();
        if (atime == null) {
            atime = mtime;
        }
        return new VirtualFileStatBuffer(
            mtime.toMillis(),
            atime.toMillis(),
            ctime.toMillis(),
            attr | (ze.isDirectory() ? VFSUtils.BA_DIRECTORY : VFSUtils.BA_REGULAR),
            ze.getSize(),
            getNodeId()
        );
    }

    @Override
    int openExisting(int fd, VirtualFileSystem vfs, DirectoryNode parent, int flags) throws IOException {
        if ((flags & VFSUtils.O_ACCESS_MODE_MASK) != VFSUtils.O_RDONLY) {
            throw notWritable();
        }
        return vfs.getVioSystem().openZipFileEntryForInput(zf, ze);
    }

    @Override
    int getMode() {
        //noinspection OctalInteger
        return 0444;
    }

    @Override
    void changeMode(int newMode) throws AccessDeniedException {
        throw notWritable();
    }

    private static AccessDeniedException notWritable() {
        return new AccessDeniedException("Zip entries are not writable");
    }
}
