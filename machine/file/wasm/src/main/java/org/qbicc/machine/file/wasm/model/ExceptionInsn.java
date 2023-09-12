package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 *
 */
public record ExceptionInsn(Op.Exception op, BranchTarget target) implements Insn<Op.Exception> {
    public ExceptionInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("target", target);
        if (target instanceof BlockInsn bi && bi.op() != Op.Block.try_) {
            throw new IllegalArgumentException("Exception instruction must target a `try` block");
        }
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(target));
    }
}
