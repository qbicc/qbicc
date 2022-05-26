package org.qbicc.machine.vfs;

/**
 *
 */
public class VirtualRoot {
    private final VirtualRootName rootName;
    private final DirectoryNode rootDirectory;

    public VirtualRoot(VirtualRootName name, VirtualFileSystem fileSystem) {
        rootName = name;
        rootDirectory = new DirectoryNode(fileSystem);
    }

    public VirtualRootName getRootName() {
        return rootName;
    }

    public DirectoryNode getRootDirectory() {
        return rootDirectory;
    }
}
