package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which operates on a global variable.
 */
public record GlobalInsn(Op.Global op, Global global) implements Insn<Op.Global> {
    public GlobalInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("global", global);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(global));
    }
}
