package org.qbicc.machine.vfs;

final class DriveLetterVirtualRootName extends VirtualRootName {
    private final String name;

    DriveLetterVirtualRootName(VirtualFileSystem fileSystem, char letter) {
        super(fileSystem);
        this.name = Character.toString(Character.toUpperCase(letter));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return super.toString(b).append(':');
    }

    @Override
    public boolean equals(VirtualRootName other) {
        return other instanceof DriveLetterVirtualRootName vrn && equals(vrn);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + name.hashCode();
    }

    @Override
    int compareTo(DriveLetterVirtualRootName other) {
        return Character.compare(name.charAt(0), other.name.charAt(0));
    }

    @Override
    int compareTo(UncVirtualRootName other) {
        return -1;
    }

    @Override
    int compareTo(UnixVirtualRootName other) {
        return 1;
    }

    boolean equals(DriveLetterVirtualRootName other) {
        return super.equals(other) && name.equals(other.name);
    }

    public char getDriveLetter() {
        return name.charAt(0);
    }
}
