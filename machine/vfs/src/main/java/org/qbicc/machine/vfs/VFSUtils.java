package org.qbicc.machine.vfs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class VFSUtils {
    private VFSUtils() {}

    public static final String DOT = ".";
    public static final String DOT_DOT = "..";

    // todo: ↓↓ deduplicate ↓↓
    public static final int O_ACCESS_MODE_MASK = 0b11;

    public static final int O_RDONLY = 0;
    public static final int O_WRONLY = 1;
    public static final int O_RDWR = 2;

    public static final int O_CREAT = 1 << 2;
    public static final int O_EXCL = 1 << 3;
    public static final int O_TRUNC = 1 << 4;
    public static final int O_APPEND = 1 << 5;

    public static final int O_DIRECTORY = 1 << 6;
    // todo: ↑↑ deduplicate ↑↑

    // constants matching what are used in java.io.FileSystem
    public static final int BA_EXISTS    = 0x01;
    public static final int BA_REGULAR   = 0x02;
    public static final int BA_DIRECTORY = 0x04;
    public static final int BA_HIDDEN    = 0x08;

    /**
     * Mount all of the entries of a zip file into the virtual file system.
     * The zip file will be closed when the virtual file system is closed.
     *
     * @param vfs the virtual file system (must not be {@code null})
     * @param mountPoint the mount point (must not be {@code null})
     * @param zipFile the zip file to mount (must not be {@code null})
     * @throws IOException if the mount fails
     */
    public static void mountZipFile(VirtualFileSystem vfs, VirtualPath mountPoint, ZipFile zipFile) throws IOException {
        Iterator<? extends ZipEntry> iterator = zipFile.entries().asIterator();
        while (iterator.hasNext()) {
            ZipEntry entry = iterator.next();
            vfs.bindZipEntry(mountPoint.resolve(entry.getName()), zipFile, entry, true);
        }
        vfs.addCloseable(zipFile);
    }

    public static void safeClose(final Closeable closeable, final Throwable t) {
        try {
            closeable.close();
        } catch (Throwable t2) {
            if (t != null) {
                t.addSuppressed(t2);
            }
        }
    }

    public static int getModeFromPermSet(Set<PosixFilePermission> set) {
        int mode = 0;
        for (PosixFilePermission perm : set) {
            mode |= 1 << perm.ordinal();
        }
        return mode;
    }

    private static final PosixFilePermission[] PERM_VALUES = PosixFilePermission.values();

    public static EnumSet<PosixFilePermission> getPermSetFromMode(int mode) {
        EnumSet<PosixFilePermission> permSet = EnumSet.noneOf(PosixFilePermission.class);
        // POSIX file permissions are in the same bit order that we use (by design)
        while (mode != 0) {
            int lob = Integer.lowestOneBit(mode);
            int idx = Integer.numberOfTrailingZeros(lob);
            if (idx < PERM_VALUES.length) {
                permSet.add(PERM_VALUES[idx]);
            }
            mode &= ~lob;
        }
        return permSet;
    }

    public static EnumSet<StandardOpenOption> getOpenOptions(int flags) throws IOException {
        int am = flags & O_ACCESS_MODE_MASK;
        EnumSet<StandardOpenOption> set = switch (am) {
            case O_RDONLY -> EnumSet.of(StandardOpenOption.READ);
            case O_WRONLY -> EnumSet.of(StandardOpenOption.WRITE);
            case O_RDWR -> EnumSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE);
            default -> throw new IOException("Invalid mode");
        };
        if ((flags & O_CREAT) != 0) {
            set.add((flags & O_EXCL) != 0 ? StandardOpenOption.CREATE_NEW : StandardOpenOption.CREATE);
        }
        if ((flags & O_TRUNC) != 0) {
            set.add(StandardOpenOption.TRUNCATE_EXISTING);
        }
        if ((flags & O_APPEND) != 0) {
            set.add(StandardOpenOption.APPEND);
        }
        return set;
    }
}
