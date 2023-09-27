package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction that models a memory access.
 */
public record MemoryAccessInsn(Op.MemoryAccess op, Memory memory, int offset, int alignment) implements Insn<Op.MemoryAccess> {
    public MemoryAccessInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("memory", memory);
        if (Integer.bitCount(alignment) != 1) {
            throw new IllegalArgumentException("Invalid alignment");
        }
        Assert.checkMaximumParameter("alignment", 1 << 16, alignment);
    }

    @Override
    public void writeTo(WasmOutputStream wos, Encoder encoder) throws IOException {
        wos.op(op);
        int memIdx = encoder.encode(memory());
        if (memIdx == 0) {
            wos.u32(Integer.numberOfTrailingZeros(alignment));
        } else {
            wos.u32(Integer.numberOfTrailingZeros(alignment) | 0b1000000);
            wos.u32(memIdx);
        }
        wos.u32(offset);
    }
}
