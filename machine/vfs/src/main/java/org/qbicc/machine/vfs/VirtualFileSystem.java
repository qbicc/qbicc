package org.qbicc.machine.vfs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.collections.api.map.sorted.ImmutableSortedMap;
import org.eclipse.collections.impl.factory.Iterables;
import org.qbicc.machine.vio.VIOSystem;

/**
 *
 */
public abstract class VirtualFileSystem implements Closeable {
    private final VIOSystem vioSystem;
    private final RelativeVirtualPath emptyPath = new RelativeVirtualPath(this, Iterables.iList());
    private final ImmutableSortedMap<String, Node> emptyMap;
    private volatile boolean closed;
    private final ArrayList<Closeable> closeables = new ArrayList<>();

    VirtualFileSystem(VIOSystem vioSystem, Comparator<? super String> segmentComparator) {
        this.vioSystem = vioSystem;
        emptyMap = Iterables.iSortedMap(segmentComparator);
    }

    public abstract VirtualRootName getDefaultRootName();

    public VIOSystem getVioSystem() {
        return vioSystem;
    }

    public abstract List<AbsoluteVirtualPath> getRootDirectories();

    public abstract VirtualPath getPath(String first, String... rest) throws IllegalArgumentException;

    abstract DirectoryNode getRootNode(VirtualRootName rootName);

    DirectoryNode getRootNode(VirtualPath vp) {
        if (vp instanceof AbsoluteVirtualPath avp) {
            return getRootNode(avp.getRootName());
        } else {
            return getRootNode(getDefaultRootName());
        }
    }

    public RelativeVirtualPath getEmptyPath() {
        return emptyPath;
    }

    public Comparator<String> getPathSegmentComparator() {
        // we always use a string comparator, so...
        //noinspection unchecked
        return (Comparator<String>) emptyMap.comparator();
    }

    public ImmutableSortedMap<String, Node> getEmptyMap() {
        return emptyMap;
    }

    public void bindExternalNode(final VirtualPath vp, final Path path, boolean replace, boolean deleteOnClose) throws IOException {
        getRootNode(vp).bindExternalNode(this, vp.relativize(), 0, path, replace, deleteOnClose);
    }

    public void bindZipEntry(final VirtualPath vp, final ZipFile zf, final ZipEntry ze, boolean replace) throws IOException {
        getRootNode(vp).bindZipEntry(this, vp.relativize(), 0, zf, ze, replace);
    }

    public void mkdirs(final VirtualPath vp, int mode) throws IOException {
        getRootNode(vp).mkdirs(this, vp.relativize(), 0, mode);
    }

    public int open(VirtualPath vp, final int flags, final int mode) throws IOException {
        return getRootNode(vp).openFile(-1, this, getRootNode(vp), vp.relativize(), 0, flags, mode);
    }

    public void open(final int fd, final VirtualPath vp, final int flags, final int mode) throws IOException {
        getRootNode(vp).openFile(fd, this, getRootNode(vp), vp.relativize(), 0, flags, mode);
    }

    public void createLink(VirtualPath vp, VirtualPath target, final Set<? extends FileAttribute<?>> attrs) throws IOException {
        throw new IOException("Unimplemented");
    }

    public VirtualPath readLink(final VirtualPath vp) throws IOException {
        return getRootNode(vp).readLink(this, vp, 0);
    }

    public void unlink(final VirtualPath vp) throws IOException {
        getRootNode(vp).unlink(this, vp.relativize(), 0);
    }

    public boolean deleteIfExists(final VirtualPath vp) throws IOException {
        try {
            unlink(vp);
            return true;
        } catch (NoSuchFileException ignored) {
            return false;
        }
    }

    public void mkdir(final VirtualPath vp, final int mode) throws IOException {
        getRootNode(vp).mkdir(this, vp.relativize(), 0, mode);
    }

    public abstract boolean isHidden(final VirtualPath path) throws IOException;

    public void checkAccess(final VirtualPath vp, boolean read, boolean write, boolean execute) throws IOException {
        getRootNode(vp).checkAccess(this, vp.relativize(), 0, read, write, execute);
    }

    public abstract String getSeparator();

    public int getBooleanAttributes(final VirtualPath vp, final boolean followLinks) throws IOException {
        return getRootNode(vp).getBooleanAttributes(vp.relativize(), 0, followLinks);
    }

    @Override
    public void close() throws IOException {
        ArrayList<Closeable> closeables = this.closeables;
        IOException e = null;
        synchronized (closeables) {
            if (closed) {
                return;
            }
            closed = true;
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (Throwable t) {
                    if (e == null) {
                        e = new IOException("One or more resources failed to close properly");
                    }
                    e.addSuppressed(t);
                }
            }
            closeables.clear();
        }
        if (e != null) {
            throw e;
        }
    }

    public void addCloseable(final Closeable closeable) throws IOException {
        ArrayList<Closeable> closeables = this.closeables;
        synchronized (closeables) {
            if (closed) {
                closeable.close();
            }
            closeables.add(closeable);
        }
    }
}
