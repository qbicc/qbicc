package org.qbicc.machine.vfs;

import static org.qbicc.machine.vfs.VFSUtils.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.smallrye.common.constraint.Assert;
import org.eclipse.collections.api.map.sorted.ImmutableSortedMap;
import org.eclipse.collections.impl.factory.Iterables;
import org.qbicc.machine.vio.DirectoryIoHandler;

final class DirectoryNode extends SingleParentNode {
    private static final ImmutableSortedMap<String, Node> REMOVED = Iterables.iSortedMap("REMOVED", null);

    private static final VarHandle entriesHandle = ConstantBootstraps.fieldVarHandle(
        MethodHandles.lookup(),
        "entries",
        VarHandle.class,
        DirectoryNode.class,
        ImmutableSortedMap.class
    );
    private static final VarHandle parentHandle = ConstantBootstraps.fieldVarHandle(
        MethodHandles.lookup(),
        "parent",
        VarHandle.class,
        DirectoryNode.class,
        DirectoryNode.class
    );

    @SuppressWarnings("FieldMayBeFinal")
    private volatile ImmutableSortedMap<String, Node> entries;

    @SuppressWarnings("FieldMayBeFinal")
    private volatile DirectoryNode parent;

    private volatile int mode;

    private DirectoryNode(ImmutableSortedMap<String, Node> initialEntries, DirectoryNode parent, int mode) {
        Assert.checkNotNullParam("parent", parent);
        entries = initialEntries;
        this.parent = parent;
        this.mode = mode;
    }

    DirectoryNode(VirtualFileSystem fileSystem) {
        // root
        entries = fileSystem.getEmptyMap();
        this.parent = this;
        //noinspection OctalInteger
        mode = 0755;
    }

    public Node get(String name) {
        return get(entries, name);
    }

    Node get(ImmutableSortedMap<String, Node> entries, String name) {
        return name.equals(VFSUtils.DOT) ? this : name.equals(VFSUtils.DOT_DOT) ? parent : entries == REMOVED ? null : entries.get(name);
    }

    @Override
    public boolean changeParent(Node oldParent, Node newParent) {
        return oldParent instanceof DirectoryNode odn && newParent instanceof DirectoryNode ndn && parentHandle.compareAndSet(this, odn, ndn);
    }

    int openFile(int fd, VirtualFileSystem vfs, DirectoryNode parent, RelativeVirtualPath rvp, int idx, int flags, int mode) throws IOException {
        int nc = rvp.getNameCount();
        if (idx == nc) {
            return openExisting(fd, vfs, parent, flags);
        } else if (idx == nc - 1) {
            return openFile(fd, vfs, rvp.getFileNameString(), flags, mode);
        } else {
            String name = rvp.getNameString(idx);
            Node node = get(name);
            if (node == null) {
                throw new FileNotFoundException(rvp.subpath(0, idx + 1).toString());
            } else if (node instanceof DirectoryNode childDn) {
                return childDn.openFile(-1, vfs, this, rvp, idx + 1, flags, mode);
            } else {
                throw nde(rvp, idx);
            }
        }
    }

    /**
     * Open a file with the given name, possibly creating it.
     *
     * @param fd the file descriptor to use, or {@code -1} to allocate one
     * @param vfs   the virtual filesystem (must not be {@code null})
     * @param name  the file entry name (must not be {@code null})
     * @param flags the open flags
     * @param mode  the file creation mode, if the file is being created
     * @return the file descriptor for the opened file
     * @throws IOException if the open fails
     */
    int openFile(int fd, VirtualFileSystem vfs, String name, int flags, int mode) throws IOException {
        boolean create = (flags & VFSUtils.O_CREAT) != 0;
        if (create && (flags & VFSUtils.O_DIRECTORY) != 0) {
            throw new AccessDeniedException(name);
        }
        if (create && ! Thread.holdsLock(this)) {
            // create is a potential read-modify-write operation
            synchronized (this) {
                return openFile(fd, vfs, name, flags, mode);
            }
        }
        boolean excl = (flags & VFSUtils.O_EXCL) != 0;
        ImmutableSortedMap<String, Node> entries = this.entries;
        Node node = get(entries, name);
        if (node == null) {
            if (! create) {
                throw new NoSuchFileException(name);
            }
            if (! isWritable()) {
                throw new AccessDeniedException(name);
            }
            node = new FileNode(vfs, mode);
            this.entries = entries.newWithKeyValue(name, node);
        } else {
            // node != null
            if (create && excl) {
                throw new FileAlreadyExistsException(name);
            }
            node.checkMode(name, mode);
        }
        return node.openExisting(fd, vfs, this, flags);
    }

    @Override
    int openExisting(int fd, VirtualFileSystem vfs, DirectoryNode parent, int flags) throws IOException {
        if ((flags & VFSUtils.O_ACCESS_MODE_MASK) != VFSUtils.O_RDONLY) {
            throw new AccessDeniedException("Cannot write to a directory");
        }
        if ((flags & (VFSUtils.O_APPEND | VFSUtils.O_TRUNC | VFSUtils.O_EXCL | VFSUtils.O_CREAT)) != 0 || (flags & VFSUtils.O_DIRECTORY) == 0) {
            throw new IOException("Invalid flags");
        }
        ImmutableSortedMap<String, Node> entries = this.entries;
        if (entries == REMOVED) {
            throw new NoSuchFileException("<deleted>");
        }
        Iterator<String> iterator = entries.keysView().iterator();
        return vfs.getVioSystem().open(() -> new DirectoryIoHandler() {
            { addLink(); }
            int stage = 0;
            private final AtomicBoolean closed = new AtomicBoolean();
            @Override
            public String readEntry() {
                return switch (bumpStage()) {
                    case 0 -> VFSUtils.DOT;
                    case 1 -> VFSUtils.DOT_DOT;
                    default -> iterator.hasNext() ? iterator.next() : null;
                };
            }

            private int bumpStage() {
                int stage = this.stage;
                switch (stage) {
                    case 0, 1 -> this.stage = stage + 1;
                }
                return stage;
            }

            @Override
            public void close() throws IOException {
                if (closed.compareAndSet(false, true)) {
                    removeLink();
                }
            }
        });
    }

    void bindZipEntry(VirtualFileSystem vfs, RelativeVirtualPath rvp, int idx, ZipFile zf, ZipEntry ze, boolean replace) throws IOException {
        int nc = rvp.getNameCount();
        if (idx == nc) {
            throw new IOException("Cannot overwrite directory");
        } else if (idx == nc - 1) {
            bindZipEntry(vfs, rvp.getNameString(idx), zf, ze, replace);
            return;
        } else {
            Node node = get(rvp.getNameString(idx));
            if (node instanceof DirectoryNode dn) {
                dn.bindZipEntry(vfs, rvp, idx + 1, zf, ze, replace);
                return;
            } else {
                throw nde(rvp, idx);
            }
        }
    }

    void bindZipEntry(VirtualFileSystem vfs, String name, ZipFile zf, ZipEntry ze, boolean replace) throws IOException {
        synchronized (this) {
            ImmutableSortedMap<String, Node> entries = this.entries;
            Node node = get(entries, name);
            if (node != null && ! replace) {
                throw new FileAlreadyExistsException(name);
            }
            if (! isWritable()) {
                throw new AccessDeniedException(name);
            }
            this.entries = entries.newWithKeyValue(name, new ZipEntryNode(zf, ze));
            if (node != null) {
                try {
                    node.removeLink();
                } catch (IOException ignored) {}
            }
        }
    }

    void bindExternalNode(final VirtualFileSystem vfs, final RelativeVirtualPath rvp, final int idx, final Path externalPath, final boolean replace, boolean deleteOnClose) throws IOException {
        int nc = rvp.getNameCount();
        if (idx == nc) {
            throw new IOException("Cannot overwrite directory");
        } else if (idx == nc - 1) {
            bindExternalNode(vfs, rvp.getNameString(idx), externalPath, replace, deleteOnClose);
            return;
        } else {
            Node node = get(rvp.getNameString(idx));
            if (node instanceof DirectoryNode dn) {
                dn.bindExternalNode(vfs, rvp, idx + 1, externalPath, replace, deleteOnClose);
                return;
            } else {
                throw nde(rvp, idx);
            }
        }
    }

    private void bindExternalNode(final VirtualFileSystem vfs, final String name, final Path externalPath, final boolean replace, boolean deleteOnClose) throws IOException {
        synchronized (this) {
            ImmutableSortedMap<String, Node> entries = this.entries;
            Node node = get(entries, name);
            if (node != null && ! replace) {
                throw new FileAlreadyExistsException(name);
            }
            if (! isWritable()) {
                throw new AccessDeniedException(name);
            }
            this.entries = entries.newWithKeyValue(name, new ExternalNode(this, externalPath, deleteOnClose));
            if (node != null) {
                try {
                    node.removeLink();
                } catch (IOException ignored) {}
            }
        }
    }

    void mkdirs(final VirtualFileSystem vfs, final RelativeVirtualPath rvp, final int idx, final int mode) throws IOException {
        if (idx == rvp.getNameCount()) {
            // done
            return;
        }
        DirectoryNode dn;
        String name = rvp.getNameString(idx);
        synchronized (this) {
            ImmutableSortedMap<String, Node> entries = this.entries;
            Node node = get(entries, name);
            if (node instanceof DirectoryNode ndn) {
                dn = ndn;
            } else {
                if (node != null) {
                    throw fae(rvp, idx);
                }
                if (! isWritable()) {
                    throw ade(rvp, idx);
                }
                this.entries = entries.newWithKeyValue(name, dn = new DirectoryNode(vfs.getEmptyMap(), this, mode));
            }
        }
        dn.mkdirs(vfs, rvp, idx + 1, mode);
    }

    void mkdir(final VirtualFileSystem vfs, final RelativeVirtualPath rvp, final int idx, final int mode) throws IOException {
        String name = rvp.getNameString(idx);
        int nc = rvp.getNameCount();
        if (idx == nc) {
            throw fae(rvp, idx);
        } else if (idx == nc - 1) {
            synchronized (this) {
                ImmutableSortedMap<String, Node> entries = this.entries;
                Node node = get(entries, name);
                if (node != null) {
                    throw fae(rvp, idx);
                }
                if (! isWritable()) {
                    throw ade(rvp, idx);
                }
                this.entries = entries.newWithKeyValue(name, new DirectoryNode(vfs.getEmptyMap(), this, mode));
            }
        } else {
            Node node = get(name);
            if (node instanceof DirectoryNode dn) {
                dn.mkdir(vfs, rvp, idx + 1, mode);
                return;
            } else {
                throw nde(rvp, idx);
            }
        }
    }

    void unlink(VirtualFileSystem vfs, RelativeVirtualPath rvp, int idx) throws IOException {
        int nc = rvp.getNameCount();
        if (idx == nc) {
            throw new IOException("Cannot unlink directory");
        } else if (idx == nc - 1) {
            unlink(rvp.getNameString(idx));
        } else {
            Node node = get(rvp.getNameString(idx));
            if (node instanceof DirectoryNode dn) {
                dn.unlink(vfs, rvp, idx + 1);
                return;
            } else {
                throw nde(rvp, idx);
            }
        }
    }

    private void unlink(String name) throws IOException {
        Assert.checkNotNullParam("name", name);
        if (name.equals(VFSUtils.DOT) || name.equals(VFSUtils.DOT_DOT)) {
            throw new IOException();
        }
        ImmutableSortedMap<String, Node> oldVal, newVal;
        Node node;
        do {
            oldVal = this.entries;
            node = oldVal.get(name);
            if (node == null) {
                // not found
                throw new NoSuchFileException(name);
            }
            if (node instanceof DirectoryNode dn) {
                // only allow removal if empty
                dn.remove();
            }
            newVal = oldVal.newWithoutKey(name);
        } while (! entriesHandle.compareAndSet(this, oldVal, newVal));
    }

    void checkAccess(final VirtualFileSystem vfs, final RelativeVirtualPath rvp, final int idx, final boolean read, final boolean write, final boolean execute) throws IOException {
        int nc = rvp.getNameCount();
        if (idx == nc) {
            checkModeFlags(rvp, idx, read, write, execute, getMode());
            return;
        }
        String name = rvp.getNameString(idx);
        Node node = get(name);
        if (node == null) {
            throw nsfe(rvp, idx);
        }
        if (idx == nc - 1) {
            checkModeFlags(rvp, idx, read, write, execute, node.getMode());
            return;
        }
        if (node instanceof DirectoryNode dn) {
            dn.checkAccess(vfs, rvp, idx + 1, read, write, execute);
            return;
        }
        throw nde(rvp, idx);
    }

    private void checkModeFlags(final RelativeVirtualPath rvp, final int idx, final boolean read, final boolean write, final boolean execute, final int mode) throws AccessDeniedException {
        //noinspection OctalInteger
        if (read & ((mode & 0444) == 0)) {
            throw ade(rvp, idx);
        }
        //noinspection OctalInteger
        if (write & ((mode & 0222) == 0)) {
            throw ade(rvp, idx);
        }
        //noinspection OctalInteger
        if (execute & ((mode & 0111) == 0)) {
            throw ade(rvp, idx);
        }
    }

    int getBooleanAttributes(final RelativeVirtualPath rvp, final int idx, final boolean followLinks) throws IOException {
        int nc = rvp.getNameCount();
        if (idx == nc) {
            return BA_EXISTS | BA_DIRECTORY;
        }
        String name = rvp.getNameString(idx);
        Node node = get(name);
        if (node == null) {
            // non-existent
            return 0;
        }
        if (idx == nc - 1) {
            // todo: maybe each node should implement this...
            if (node instanceof DirectoryNode) {
                return BA_EXISTS | BA_DIRECTORY;
            } else if (node instanceof ExternalNode en) {
                BasicFileAttributeView av = Files.getFileAttributeView(en.getExternalPath(), BasicFileAttributeView.class);
                if (av == null) {
                    return Files.exists(en.getExternalPath()) ? BA_EXISTS : 0;
                }
                BasicFileAttributes baf = av.readAttributes();
                int val = BA_EXISTS;
                if (baf.isRegularFile()) {
                    val |= BA_REGULAR;
                }
                if (baf.isDirectory()) {
                    val |= BA_DIRECTORY;
                }
                return val;
            } else {
                return BA_EXISTS | BA_REGULAR;
            }
        }
        if (node instanceof DirectoryNode dn) {
            return dn.getBooleanAttributes(rvp, idx, followLinks);
        }
        throw nde(rvp, idx);
    }

    private void remove() throws IOException {
        ImmutableSortedMap<String, Node> entries = this.entries;
        if (entries == REMOVED || entries.isEmpty() && entriesHandle.compareAndSet(entries, REMOVED)) {
            return;
        }
        throw new DirectoryNotEmptyException(null);
    }

    Node link(String name, Node value) throws IOException {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("value", value);
        if (name.equals(VFSUtils.DOT) || name.equals(VFSUtils.DOT_DOT)) {
            throw new IOException();
        }
        if (value instanceof DirectoryNode dn && dn.getParent() != this) {
            throw new IOException("Node cannot be linked here");
        }
        ImmutableSortedMap<String, Node> oldVal, newVal;
        Node node;
        do {
            oldVal = this.entries;
            if (oldVal == REMOVED) {
                throw new NoSuchFileException(name);
            }
            node = oldVal.get(name);
            if (node instanceof DirectoryNode dn) {
                // only allow removal if empty
                dn.remove();
            }
            newVal = oldVal.newWithKeyValue(name, value);
        } while (! entriesHandle.compareAndSet(this, oldVal, newVal));
        return node;
    }

    Node linkIfAbsent(String name, Node value) throws IOException {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("value", value);
        if (name.equals(VFSUtils.DOT) || name.equals(VFSUtils.DOT_DOT)) {
            throw new IOException();
        }
        if (value instanceof DirectoryNode dn && dn.getParent() != this) {
            throw new IOException("Node cannot be linked here");
        }
        ImmutableSortedMap<String, Node> oldVal, newVal;
        Node node;
        do {
            oldVal = this.entries;
            if (oldVal == REMOVED) {
                throw new NoSuchFileException(name);
            }
            node = oldVal.get(name);
            if (node != null) {
                // already present
                return node;
            }
            newVal = oldVal.newWithKeyValue(name, value);
        } while (! entriesHandle.compareAndSet(this, oldVal, newVal));
        return null;
    }

    Node linkIfPresent(String name, Node value) throws IOException {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("value", value);
        if (name.equals(VFSUtils.DOT) || name.equals(VFSUtils.DOT_DOT)) {
            throw new IOException();
        }
        if (value instanceof DirectoryNode dn && dn.getParent() != this) {
            throw new IOException("Node cannot be linked here");
        }
        ImmutableSortedMap<String, Node> oldVal, newVal;
        Node node;
        do {
            oldVal = this.entries;
            if (oldVal == REMOVED) {
                throw new NoSuchFileException(name);
            }
            node = oldVal.get(name);
            if (node == null) {
                // not present
                return null;
            }
            newVal = oldVal.newWithKeyValue(name, value);
        } while (! entriesHandle.compareAndSet(this, oldVal, newVal));
        return node;
    }

    public ImmutableSortedMap<String, Node> getEntries() {
        return entries;
    }

    public DirectoryNode getParent() {
        return parent;
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

    private static AccessDeniedException ade(final RelativeVirtualPath rvp, final int idx) {
        return new AccessDeniedException(rvp.subpath(0, idx).toString());
    }

    private static FileAlreadyExistsException fae(final RelativeVirtualPath rvp, final int idx) {
        return new FileAlreadyExistsException(rvp.subpath(0, idx + 1).toString());
    }

    private static NoSuchFileException nsfe(final RelativeVirtualPath rvp, final int idx) {
        return new NoSuchFileException(rvp.subpath(0, idx + 1).toString());
    }

    private static NotDirectoryException nde(final RelativeVirtualPath rvp, final int idx) {
        return new NotDirectoryException(rvp.subpath(0, idx + 1).toString());
    }
}
