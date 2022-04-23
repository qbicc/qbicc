package org.qbicc.machine.file.wasm;

import java.io.IOException;
import java.nio.file.Path;

import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.file.bin.BinaryBuffer;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;

/**
 *
 */
public final class WasmObjectFileProvider implements ObjectFileProvider {
    public WasmObjectFileProvider() {}

    public ObjectFile openObjectFile(final Path path) throws IOException {
        return new WasmObjectFile();
    }

    public ObjectType getObjectType() {
        return ObjectType.WASM;
    }
}
