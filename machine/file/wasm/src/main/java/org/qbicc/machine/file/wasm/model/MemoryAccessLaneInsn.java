package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction that models a memory access involving a vector lane.
 */
public record MemoryAccessLaneInsn(Op.MemoryAccessLane op, Memory memory, int offset, int alignment, int lane) implements Insn<Op.MemoryAccessLane> {
    public MemoryAccessLaneInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("memory", memory);
        if (Integer.bitCount(alignment) != 1) {
            throw new IllegalArgumentException("Invalid alignment");
        }
        Assert.checkMaximumParameter("alignment", 1 << 16, alignment);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        int memIdx = encoder.encode(memory());
        if (memIdx == 0) {
            wos.u32(Integer.numberOfTrailingZeros(alignment));
        } else {
            wos.u32(Integer.numberOfTrailingZeros(alignment) | 0b1000000);
            wos.u32(memIdx);
        }
        wos.u32(offset);
        wos.u32(lane);
    }
}
