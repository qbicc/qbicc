package org.qbicc.machine.vfs;

final class UnixVirtualRootName extends VirtualRootName {
    UnixVirtualRootName(VirtualFileSystem fileSystem) {
        super(fileSystem);
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public boolean equals(VirtualRootName other) {
        return other instanceof UnixVirtualRootName vrn && equals(vrn);
    }

    @Override
    int compareTo(DriveLetterVirtualRootName other) {
        return -1;
    }

    @Override
    int compareTo(UncVirtualRootName other) {
        return -1;
    }

    int compareTo(final UnixVirtualRootName other) {
        return 0;
    }

    boolean equals(UnixVirtualRootName other) {
        return super.equals(other);
    }
}
