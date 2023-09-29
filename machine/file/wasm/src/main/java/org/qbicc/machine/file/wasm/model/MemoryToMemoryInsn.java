package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which operates on two memory instances (destination and source).
 */
public record MemoryToMemoryInsn(Op.MemoryToMemory op, Memory destination, Memory source) implements Insn<Op.MemoryToMemory>, Cacheable {
    public MemoryToMemoryInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("destination", destination);
        Assert.checkNotNullParam("source", source);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(destination()));
        wos.u32(encoder.encode(source()));
    }
}
