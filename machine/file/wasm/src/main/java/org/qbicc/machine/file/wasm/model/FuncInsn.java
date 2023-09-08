package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which takes a function as its argument.
 */
public record FuncInsn(Op.Func op, Func func) implements Insn<Op.Func> {
    public FuncInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("func", func);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(func));
    }
}
