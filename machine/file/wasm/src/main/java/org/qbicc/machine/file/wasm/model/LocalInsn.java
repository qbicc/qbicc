package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 *
 */
public record LocalInsn(Op.Local op, int localIdx) implements Insn<Op.Local> {
    public LocalInsn {
        Assert.checkNotNullParam("op", op);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, localIdx);
    }
}
