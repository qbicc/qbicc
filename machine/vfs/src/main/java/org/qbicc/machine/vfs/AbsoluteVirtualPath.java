package org.qbicc.machine.vfs;

import org.eclipse.collections.api.list.ImmutableList;

public final class AbsoluteVirtualPath extends VirtualPath {
    private final VirtualRootName root;

    AbsoluteVirtualPath(VirtualRootName root, ImmutableList<String> segments) {
        super(root.getFileSystem(), segments);
        this.root = root;
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    public VirtualRootName getRootName() {
        return root;
    }

    @Override
    public AbsoluteVirtualPath getRoot() {
        return root.getRootPath();
    }

    @Override
    public AbsoluteVirtualPath toAbsolutePath() {
        return this;
    }

    @Override
    public AbsoluteVirtualPath getParent() {
        return (AbsoluteVirtualPath) super.getParent();
    }

    @Override
    public boolean startsWith(AbsoluteVirtualPath avp) {
        ImmutableList<String> otherSegments = avp.segments;
        ImmutableList<String> segments = this.segments;
        int otherSize = otherSegments.size();
        int size = segments.size();
        return root == avp.root && otherSize <= size && segments.subList(0, otherSize).equals(otherSegments);
    }

    @Override
    public boolean startsWith(RelativeVirtualPath rvp) {
        return false;
    }

    @Override
    public boolean endsWith(AbsoluteVirtualPath avp) {
        ImmutableList<String> otherSegments = avp.segments;
        ImmutableList<String> segments = this.segments;
        int otherSize = otherSegments.size();
        int size = segments.size();
        return (otherSize < size || otherSize == size && avp.root == root) && segments.subList(size - otherSize, size).equals(otherSegments);
    }

    @Override
    public AbsoluteVirtualPath normalize() {
        return (AbsoluteVirtualPath) super.normalize();
    }

    @Override
    public AbsoluteVirtualPath resolve(VirtualPath other) {
        return (AbsoluteVirtualPath) super.resolve(other);
    }

    @Override
    public AbsoluteVirtualPath resolve(String other) {
        return (AbsoluteVirtualPath) super.resolve(other);
    }

    @Override
    public AbsoluteVirtualPath resolve(RelativeVirtualPath rvp) {
        return copy(segments.newWithAll(rvp.segments));
    }

    @Override
    RelativeVirtualPath relativize() {
        return new RelativeVirtualPath(getFileSystem(), segments);
    }

    @Override
    RelativeVirtualPath relativize(AbsoluteVirtualPath avp) {
        return root == avp.root ? relativizeCommon(avp) : cannotRelativize();
    }

    @Override
    RelativeVirtualPath relativize(RelativeVirtualPath rvp) {
        return cannotRelativize();
    }

    @Override
    public boolean equals(VirtualPath virtualPath) {
        return virtualPath instanceof AbsoluteVirtualPath avp && equals(avp);
    }

    @Override
    public int hashCode() {
        return root.hashCode() * 19 + super.hashCode();
    }

    boolean equals(AbsoluteVirtualPath avp) {
        return super.equals(avp) && root == avp.root;
    }

    @Override
    AbsoluteVirtualPath copy(ImmutableList<String> newSegments) {
        return newSegments.isEmpty() ? root.getRootPath() : new AbsoluteVirtualPath(root, newSegments);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        root.toString(b);
        b.append(getFileSystem().getSeparator());
        return super.toString(b);
    }

    @Override
    public int compareTo(AbsoluteVirtualPath other) {
        return compareSegmentsTo(other.segments);
    }

    @Override
    public int compareTo(RelativeVirtualPath other) {
        // relative comes before absolute
        return 1;
    }
}
