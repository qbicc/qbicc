package org.qbicc.machine.vfs;

import java.util.List;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.qbicc.machine.vio.VIOSystem;

/**
 *
 */
public class PosixVirtualFileSystem extends VirtualFileSystem {
    private final UnixVirtualRootName rootName;
    private final DirectoryNode rootNode;
    private final List<AbsoluteVirtualPath> rootList;

    public PosixVirtualFileSystem(VIOSystem vioSystem, boolean caseSensitive) {
        super(vioSystem, caseSensitive ? String::compareTo : String::compareToIgnoreCase);
        rootName = new UnixVirtualRootName(this);
        rootNode = new DirectoryNode(this);
        rootList = List.of(rootName.getRootPath());
    }

    @Override
    public VirtualRootName getDefaultRootName() {
        return rootName;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public List<AbsoluteVirtualPath> getRootDirectories() {
        return rootList;
    }

    private static final int ST_INITIAL = 0;
    private static final int ST_SLASH = 1;
    private static final int ST_SEGMENT = 2;

    @Override
    public VirtualPath getPath(String first, String... rest) throws IllegalArgumentException {
        return parsePosixPath(this, first, rest);
    }

    static VirtualPath parsePosixPath(final VirtualFileSystem vfs, final String first, final String... rest) {
        int state = ST_INITIAL;
        String current = first;
        int strIdx = 0;
        int nextIdx = 0;
        int start = -1;
        VirtualRootName rootName = null;
        MutableList<String> segments = FastList.newList();
        for (;;) {
            if (strIdx == current.length() && state == ST_SEGMENT) {
                segments.add(current.substring(start, strIdx));
                // virtual slash
                state = ST_SLASH;
            }
            while (current == null || strIdx == current.length()) {
                if (rest != null && nextIdx < rest.length) {
                    current = rest[nextIdx++];
                    strIdx = 0;
                } else {
                    // done!
                    if (rootName != null) {
                        if (segments.isEmpty()) {
                            return rootName.getRootPath();
                        } else {
                            return new AbsoluteVirtualPath(rootName, segments.toImmutable());
                        }
                    } else {
                        if (segments.isEmpty()) {
                            return vfs.getEmptyPath();
                        } else {
                            return new RelativeVirtualPath(vfs, segments.toImmutable());
                        }
                    }
                }
            }
            int c = current.charAt(strIdx);
            switch (c) {
                case '/' -> {
                    switch (state) {
                        case ST_INITIAL -> rootName = vfs.getDefaultRootName();
                        case ST_SLASH -> {}
                        case ST_SEGMENT -> segments.add(current.substring(start, strIdx));
                        default -> throw new IllegalStateException();
                    }
                    state = ST_SLASH;
                }
                default -> {
                    switch (state) {
                        case ST_INITIAL, ST_SLASH -> start = strIdx;
                        case ST_SEGMENT -> {}
                        default -> throw new IllegalStateException();
                    }
                    state = ST_SEGMENT;
                }
            }
            strIdx++;
        }
    }

    @Override
    DirectoryNode getRootNode(VirtualRootName rootName) {
        return rootName.equals(this.rootName) ? rootNode : null;
    }

    public boolean isHidden(final VirtualPath path) {
        return path.getFileNameString().startsWith(".");
    }
}
