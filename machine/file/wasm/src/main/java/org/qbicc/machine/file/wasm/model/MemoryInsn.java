package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which operates on some memory instance.
 */
public record MemoryInsn(Op.Memory op, Memory memory) implements Insn<Op.Memory>, Cacheable {
    public MemoryInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("memory", memory);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(memory()));
    }
}
