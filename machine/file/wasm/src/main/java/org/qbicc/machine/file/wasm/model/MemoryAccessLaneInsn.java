package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction that models a memory access involving a vector lane.
 */
public record MemoryAccessLaneInsn(Op.MemoryAccessLane op, Memory memory, int offset, int alignment, int lane) implements Insn<Op.MemoryAccessLane> {
    public MemoryAccessLaneInsn {
        Assert.checkNotNullParam("op", op);
        if (Integer.bitCount(alignment) != 1) {
            throw new IllegalArgumentException("Invalid alignment");
        }
        Assert.checkMaximumParameter("alignment", 1 << 6, alignment);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(memory), offset, alignment, lane);
    }
}
