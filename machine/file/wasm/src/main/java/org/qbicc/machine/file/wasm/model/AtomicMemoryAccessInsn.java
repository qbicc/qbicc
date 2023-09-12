package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

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

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(memory), offset);
    }
}
