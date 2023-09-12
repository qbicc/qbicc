package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which operates on a table.
 */
public record TableInsn(Op.Table op, Table table) implements Insn<Op.Table> {
    public TableInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("table", table);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(table));
    }
}
