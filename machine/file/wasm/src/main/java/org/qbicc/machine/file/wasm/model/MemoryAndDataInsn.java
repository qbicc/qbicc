package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which operates on some memory instance and a data segment.
 */
public record MemoryAndDataInsn(Op.MemoryAndData op, Memory memory, Segment segment) implements Insn<Op.MemoryAndData>, Cacheable {
    public MemoryAndDataInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("memory", memory);
        Assert.checkNotNullParam("segment", segment);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(segment()));
        wos.u32(encoder.encode(memory()));
    }
}
