package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which operates on some memory instance.
 */
public record MemoryInsn(Op.Memory op, Memory memory) implements Insn<Op.Memory> {
    public MemoryInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("memory", memory);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(memory));
    }
}
