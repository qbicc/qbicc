package org.qbicc.machine.vfs;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 */
final class ZipEntryNode extends Node {
    private final ZipFile zf;
    private final ZipEntry ze;

    ZipEntryNode(ZipFile zf, ZipEntry ze) {
        this.zf = zf;
        this.ze = ze;
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
