package org.qbicc.machine.vfs;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

/**
 * A path name in the virtual file system of the hosted application which is being built.
 */
public abstract class VirtualPath implements Comparable<VirtualPath> {
    private final VirtualFileSystem fileSystem;
    final ImmutableList<String> segments;

    private transient String toString;

    VirtualPath(VirtualFileSystem fileSystem, ImmutableList<String> segments) {
        this.fileSystem = fileSystem;
        this.segments = segments;
    }

    public VirtualFileSystem getFileSystem() {
        return fileSystem;
    }

    public VirtualPath getFileName() {
        return getName(segments.size() - 1);
    }

    public String getFileNameString() {
        return segments.getLast();
    }

    public VirtualPath getParent() {
        return segments.isEmpty() ? null : subpath(0, segments.size() - 1);
    }

    public int getNameCount() {
        return segments.size();
    }

    public VirtualPath getName(final int index) {
        return subpath(index, index + 1);
    }

    public String getNameString(final int index) {
        return segments.get(index);
    }

    public Iterable<String> getNameStrings() {
        return segments;
    }

    public VirtualPath subpath(final int beginIndex, final int endIndex) {
        ImmutableList<String> newSegments = segments.subList(beginIndex, endIndex);
        return beginIndex == 0 ? copy(newSegments) : new RelativeVirtualPath(fileSystem, newSegments);
    }

    public VirtualPath subpath(final int beginIndex) {
        ImmutableList<String> newSegments = segments.subList(beginIndex, segments.size());
        return beginIndex == 0 ? copy(newSegments) : new RelativeVirtualPath(fileSystem, newSegments);
    }

    public boolean startsWith(VirtualPath other) {
        return other instanceof AbsoluteVirtualPath avp && startsWith(avp) || other instanceof RelativeVirtualPath rvp && startsWith(rvp);
    }

    public abstract boolean startsWith(AbsoluteVirtualPath avp);

    public abstract boolean startsWith(RelativeVirtualPath rvp);

    public final boolean endsWith(final VirtualPath other) {
        return other instanceof AbsoluteVirtualPath avp && endsWith(avp) || other instanceof RelativeVirtualPath rvp && endsWith(rvp);
    }

    public abstract boolean endsWith(AbsoluteVirtualPath avp);

    public boolean endsWith(RelativeVirtualPath rvp) {
        ImmutableList<String> otherSegments = rvp.segments;
        ImmutableList<String> segments = this.segments;
        int otherSize = otherSegments.size();
        int size = segments.size();
        return otherSize <= size && segments.subList(size - otherSize, size).equals(otherSegments);
    }

    public VirtualPath normalize() {
        ListIterator<String> li = segments.listIterator();
        MutableList<String> ml = null;
        while (li.hasNext()) {
            String item = li.next();
            boolean isDot = item.equals(VFSUtils.DOT);
            boolean isDotDot = item.equals(VFSUtils.DOT_DOT);
            if (isDot || isDotDot) {
                if (ml == null) {
                    ml = new FastList<>(segments.size());
                    int idx = li.nextIndex();
                    segments.forEach(0, idx, ml::add);
                }
            }
            if (isDot) {
                // skip segment
            } else if (isDotDot) {
                int size = ml.size();
                if (size > 0) {
                    ml.remove(size - 1);
                }
                // else skip segment
            } else {
                // add segment
                if (ml != null) {
                    ml.add(item);
                }
            }
        }
        if (ml == null) {
            // already normalized
            return this;
        } else {
            return copy(ml.toImmutable());
        }
    }

    public VirtualPath resolve(final VirtualPath other) {
        return other instanceof AbsoluteVirtualPath avp ? resolve(avp) : other instanceof RelativeVirtualPath rvp ? resolve(rvp) : wrongType();
    }

    public VirtualPath resolve(final String other) {
        return resolve(getFileSystem().getPath(other));
    }

    public AbsoluteVirtualPath resolve(final AbsoluteVirtualPath avp) {
        return avp;
    }

    public abstract VirtualPath resolve(final RelativeVirtualPath rvp);

    static <T> T wrongType() {
        throw new IllegalArgumentException("Wrong Path type for this provider");
    }

    public RelativeVirtualPath relativize(final VirtualPath other) {
        return other instanceof AbsoluteVirtualPath avp ? relativize(avp) : other instanceof RelativeVirtualPath rvp ? relativize(rvp) : cannotRelativize();
    }

    RelativeVirtualPath relativizeCommon(final VirtualPath other) {
        // always start with normalized paths
        ImmutableList<String> otherSegments = other.normalize().segments;
        ImmutableList<String> segments = normalize().segments;
        int otherSize = otherSegments.size();
        int size = segments.size();
        // first skip all common elements
        ListIterator<String> iter = segments.listIterator();
        ListIterator<String> otherIter = otherSegments.listIterator();
        while (iter.hasNext() && otherIter.hasNext()) {
            String next = iter.next();
            String otherNext = otherIter.next();
            if (! next.equals(otherNext)) {
                // the paths diverge here
                //   a/b/c/...
                //   a/b/d/...
                //       ^
                // insert enough .. segments to back all the way out of our remaining paths
                int cnt = iter.nextIndex();
                MutableList<String> ml = new FastList<>(otherSize - cnt + size - cnt);
                ml.addAll(Collections.nCopies(size - cnt, VFSUtils.DOT_DOT));
                // now add the remaining parts of other
                otherIter.forEachRemaining(ml::add);
                return new RelativeVirtualPath(fileSystem, ml.toImmutable());
            }
            // otherwise the paths match so far
        }
        // end of one or both paths
        if (iter.hasNext()) {
            // we have more segments than other
            // just return enough .. to back all the way out
            int cnt = iter.nextIndex();
            return new RelativeVirtualPath(fileSystem, Lists.immutable.ofAll(Collections.nCopies(size - cnt, VFSUtils.DOT_DOT)));
        } else if (otherIter.hasNext()) {
            // other has more segments than us
            // just return the sublist
            return new RelativeVirtualPath(fileSystem, otherSegments.subList(size, otherSize));
        } else {
            // they are identical
            return fileSystem.getEmptyPath();
        }
    }

    abstract RelativeVirtualPath relativize();

    abstract RelativeVirtualPath relativize(AbsoluteVirtualPath avp);

    abstract RelativeVirtualPath relativize(RelativeVirtualPath rvp);

    public abstract boolean isAbsolute();

    public abstract AbsoluteVirtualPath getRoot();

    public abstract AbsoluteVirtualPath toAbsolutePath();

    @Override
    public int compareTo(final VirtualPath other) {
        return other instanceof AbsoluteVirtualPath avp ? compareTo(avp) : other instanceof RelativeVirtualPath rvp ? compareTo(rvp) : classCastInt();
    }

    public abstract int compareTo(final AbsoluteVirtualPath other);

    public abstract int compareTo(final RelativeVirtualPath other);

    int compareSegmentsTo(final ImmutableList<String> otherSegments) {
        ImmutableList<String> segments = this.segments;
        int size = segments.size();
        int otherSize = otherSegments.size();
        int minSize = Math.min(size, otherSize);
        int res;
        Comparator<String> cmp = getFileSystem().getPathSegmentComparator();
        for (int i = 0; i < minSize; i ++) {
            res = cmp.compare(segments.get(i), otherSegments.get(i));
            if (res != 0) {
                return res;
            }
        }
        // shortest first
        return Integer.compare(size, otherSize);
    }

    private int classCastInt() {
        throw new ClassCastException("Wrong Path type for this provider");
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof VirtualPath vp && equals(vp);
    }

    public boolean equals(VirtualPath virtualPath) {
        return virtualPath == this || virtualPath != null && fileSystem.equals(virtualPath.fileSystem) && segments.equals(virtualPath.segments);
    }

    public int hashCode() {
        return fileSystem.hashCode() * 19 + segments.hashCode();
    }

    abstract VirtualPath copy(ImmutableList<String> newSegments);

    static <T> T cannotRelativize() {
        throw new IllegalArgumentException("The given path cannot be relativized against this path");
    }

    @Override
    public String toString() {
        String toString = this.toString;
        if (toString == null) {
            toString = this.toString = toString(new StringBuilder()).toString();
        }
        return toString;
    }

    public StringBuilder toString(StringBuilder b) {
        Iterator<String> iterator = segments.iterator();
        if (iterator.hasNext()) {
            String sep = fileSystem.getSeparator();
            b.append(iterator.next());
            while (iterator.hasNext()) {
                b.append(sep);
                b.append(iterator.next());
            }
        }
        return b;
    }
}
