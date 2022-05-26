package org.qbicc.machine.vfs;

import java.io.IOException;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Set;

final class ExternalNode extends SingleParentNode {
    private static final VarHandle parentHandle = ConstantBootstraps.fieldVarHandle(
        MethodHandles.lookup(),
        "parent",
        VarHandle.class,
        ExternalNode.class,
        Node.class
    );
    @SuppressWarnings("FieldMayBeFinal")
    private volatile Node parent;
    private final Path path;
    private final boolean deleteOnClose;

    ExternalNode(Node parent, final Path path, boolean deleteOnClose) {
        this.parent = parent;
        this.path = path;
        this.deleteOnClose = deleteOnClose;
    }

    public Path getExternalPath() {
        return path;
    }

    public void close() throws IOException {
        if (deleteOnClose) {
            Files.delete(getExternalPath());
        }
    }

    @Override
    int getMode() {
        try {
            return VFSUtils.getModeFromPermSet(Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS));
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    void changeMode(int newMode) throws AccessDeniedException {
        try {
            Files.setPosixFilePermissions(path, VFSUtils.getPermSetFromMode(newMode));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (IOException e) {
            throw new AccessDeniedException(path.toString(), null, e.toString());
        }
    }

    @Override
    int openExisting(int fd, VirtualFileSystem vfs, DirectoryNode parent, int flags) throws IOException {
        EnumSet<StandardOpenOption> set = VFSUtils.getOpenOptions(flags);
        set.remove(StandardOpenOption.CREATE);
        set.remove(StandardOpenOption.CREATE_NEW);
        if (fd == -1) {
            return vfs.getVioSystem().openRealFile(path, set, Set.of());
        } else {
            vfs.getVioSystem().openRealFile(fd, path, set, Set.of());
            return fd;
        }
    }

    @Override
    VirtualPath readLink(VirtualFileSystem fileSystem, VirtualPath vp, int vpi) throws IOException {
        return fileSystem.getPath(Files.readSymbolicLink(this.path).toString());
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public boolean changeParent(Node oldParent, Node newParent) {
        return newParent != null && parentHandle.compareAndSet(this, oldParent, newParent);
    }
}
