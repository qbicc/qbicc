package org.qbicc.machine.vfs;

import java.util.Objects;

import io.smallrye.common.constraint.Assert;
import org.eclipse.collections.impl.factory.Iterables;

/**
 * A name of a virtual root.
 */
public abstract class VirtualRootName implements Comparable<VirtualRootName> {
    private final AbsoluteVirtualPath rootPath;
    private final VirtualFileSystem fileSystem;

    VirtualRootName(VirtualFileSystem fileSystem) {
        this.fileSystem = fileSystem;
        rootPath = new AbsoluteVirtualPath(this, Iterables.iList());
    }

    public abstract String getName();

    public AbsoluteVirtualPath getRootPath() {
        return rootPath;
    }

    public VirtualFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(final StringBuilder b) {
        return b.append(getName());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VirtualRootName vrn && equals(vrn);
    }

    public boolean equals(VirtualRootName other) {
        return this == other || other != null && rootPath.equals(other.rootPath) && fileSystem == other.fileSystem;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootPath, fileSystem);
    }

    @Override
    public int compareTo(VirtualRootName o) {
        if (o instanceof DriveLetterVirtualRootName vrn) {
            return compareTo(vrn);
        } else if (o instanceof UncVirtualRootName vrn) {
            return compareTo(vrn);
        } else if (o instanceof UnixVirtualRootName vrn) {
            return compareTo(vrn);
        } else {
            throw Assert.unreachableCode();
        }
    }

    abstract int compareTo(DriveLetterVirtualRootName other);

    abstract int compareTo(UncVirtualRootName other);

    abstract int compareTo(UnixVirtualRootName other);
}
