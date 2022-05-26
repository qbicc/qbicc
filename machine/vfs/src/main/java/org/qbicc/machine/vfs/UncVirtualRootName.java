package org.qbicc.machine.vfs;

import io.smallrye.common.constraint.Assert;

final class UncVirtualRootName extends VirtualRootName {
    private final String serverName;

    UncVirtualRootName(VirtualFileSystem fileSystem, String serverName) {
        super(fileSystem);
        this.serverName = Assert.checkNotEmptyParam("serverName", serverName);
    }

    @Override
    public String getName() {
        return serverName;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        String sep = getFileSystem().getSeparator();
        b.append(sep);
        b.append(sep);
        return super.toString(b);
    }

    @Override
    public boolean equals(VirtualRootName other) {
        return other instanceof UncVirtualRootName vrn && equals(vrn);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + serverName.hashCode();
    }

    @Override
    int compareTo(DriveLetterVirtualRootName other) {
        return 1;
    }

    @Override
    int compareTo(UncVirtualRootName other) {
        return serverName.compareToIgnoreCase(other.serverName);
    }

    int compareTo(final UnixVirtualRootName other) {
        return 1;
    }

    boolean equals(UncVirtualRootName other) {
        return super.equals(other) && serverName.equalsIgnoreCase(other.serverName);
    }
}
