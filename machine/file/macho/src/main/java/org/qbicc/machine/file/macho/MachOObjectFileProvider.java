package org.qbicc.machine.file.macho;

import java.io.IOException;
import java.nio.file.Path;

import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.file.bin.BinaryBuffer;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;

/**
 *
 */
public final class MachOObjectFileProvider implements ObjectFileProvider {
    public MachOObjectFileProvider() {}

    public ObjectFile openObjectFile(final Path path) throws IOException {
        return new MachOObjectFile(BinaryBuffer.openRead(path));
    }

    public ObjectType getObjectType() {
        return ObjectType.macho;
    }
}
