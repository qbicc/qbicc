package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which takes a constant value argument.
 */
public record ConstF32Insn(Op.ConstF32 op, float val) implements Insn<Op.ConstF32> {
    public ConstF32Insn {
        Assert.checkNotNullParam("op", op);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, val);
    }
}
