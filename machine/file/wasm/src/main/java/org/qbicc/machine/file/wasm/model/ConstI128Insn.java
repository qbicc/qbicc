package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which takes a constant value argument.
 */
public record ConstI128Insn(Op.ConstI128 op, long low, long high) implements Insn<Op.ConstI128> {
    public ConstI128Insn {
        Assert.checkNotNullParam("op", op);
    }

    public ConstI128Insn(final Op.ConstI128 op, final long low) {
        this(op, low, 0);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, low, high);
    }
}
