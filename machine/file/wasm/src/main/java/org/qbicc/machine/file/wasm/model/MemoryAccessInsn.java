package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction that models a memory access.
 */
public record MemoryAccessInsn(Op.MemoryAccess op, Memory memory, int offset, int alignment) implements Insn<Op.MemoryAccess> {
    public MemoryAccessInsn {
        Assert.checkNotNullParam("op", op);
        if (Integer.bitCount(alignment) != 1) {
            throw new IllegalArgumentException("Invalid alignment");
        }
        Assert.checkMaximumParameter("alignment", 1 << 6, alignment);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(memory), alignment, offset);
    }
}
