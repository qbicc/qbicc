package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction that models a memory access.
 */
public record AtomicMemoryAccessInsn(Op.AtomicMemoryAccess op, Memory memory, int offset) implements Insn<Op.AtomicMemoryAccess> {
    public AtomicMemoryAccessInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("memory", memory);
    }

    public int alignment() {
        return op.alignment();
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(memory()));
    }
}
