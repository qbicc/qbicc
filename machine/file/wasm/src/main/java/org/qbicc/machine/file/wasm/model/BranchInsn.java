package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which branches to some enclosing label.
 */
public record BranchInsn(Op.Branch op, BranchTarget target) implements Insn<Op.Branch> {
    public BranchInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("target", target);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(target));
    }
}
