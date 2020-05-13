package cc.quarkus.qcc.machine.file.macho;

import java.io.IOException;
import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.ObjectType;
import cc.quarkus.qcc.machine.file.bin.BinaryBuffer;
import cc.quarkus.qcc.machine.object.ObjectFile;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;

/**
 *
 */
public final class MachOObjectFileProvider implements ObjectFileProvider {
    public MachOObjectFileProvider() {}

    public ObjectFile openObjectFile(final Path path) throws IOException {
        return new MachOObjectFile(BinaryBuffer.openRead(path));
    }

    public ObjectType getObjectType() {
        return ObjectType.MACH_O;
    }
}
