package org.qbicc.machine.vfs;

import org.eclipse.collections.api.list.ImmutableList;

public final class RelativeVirtualPath extends VirtualPath {
    RelativeVirtualPath(VirtualFileSystem fileSystem, ImmutableList<String> segments) {
        super(fileSystem, segments);
    }

    @Override
    public RelativeVirtualPath getParent() {
        return (RelativeVirtualPath) super.getParent();
    }

    @Override
    public boolean startsWith(AbsoluteVirtualPath avp) {
        return false;
    }

    @Override
    public boolean startsWith(RelativeVirtualPath rvp) {
        ImmutableList<String> otherSegments = rvp.segments;
        ImmutableList<String> segments = this.segments;
        int otherSize = otherSegments.size();
        int size = segments.size();
        return otherSize <= size && segments.subList(0, otherSize).equals(otherSegments);
    }

    @Override
    public boolean endsWith(AbsoluteVirtualPath avp) {
        return false;
    }

    @Override
    public RelativeVirtualPath normalize() {
        return (RelativeVirtualPath) super.normalize();
    }

    public RelativeVirtualPath resolve(final RelativeVirtualPath rvp) {
        return copy(segments.newWithAll(rvp.segments));
    }

    @Override
    RelativeVirtualPath relativize() {
        return this;
    }

    @Override
    RelativeVirtualPath relativize(AbsoluteVirtualPath avp) {
        return cannotRelativize();
    }

    @Override
    RelativeVirtualPath relativize(RelativeVirtualPath rvp) {
        return relativizeCommon(rvp);
    }

    @Override
    public boolean equals(VirtualPath virtualPath) {
        return virtualPath instanceof RelativeVirtualPath rvp && equals(rvp);
    }

    public boolean equals(RelativeVirtualPath rvp) {
        return super.equals(rvp);
    }

    @Override
    RelativeVirtualPath copy(ImmutableList<String> newSegments) {
        return newSegments.isEmpty() ? getFileSystem().getEmptyPath() : new RelativeVirtualPath(getFileSystem(), newSegments);
    }

    @Override
    public boolean isAbsolute() {
        return false;
    }

    @Override
    public AbsoluteVirtualPath getRoot() {
        return null;
    }

    @Override
    public AbsoluteVirtualPath toAbsolutePath() {
        return getFileSystem().getDefaultRootName().getRootPath().resolve(this);
    }

    @Override
    public int compareTo(AbsoluteVirtualPath other) {
        // relative comes before absolute
        return -1;
    }

    @Override
    public int compareTo(RelativeVirtualPath other) {
        return compareSegmentsTo(other.segments);
    }
}
