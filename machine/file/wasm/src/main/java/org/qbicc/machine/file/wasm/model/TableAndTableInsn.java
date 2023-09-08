package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which operates on a table.
 */
public record TableAndTableInsn(Op.TableAndTable op, Table table1, Table table2) implements Insn<Op.TableAndTable> {
    public TableAndTableInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("table1", table1);
        Assert.checkNotNullParam("table2", table2);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(table1), encoder.encode(table2));
    }
}
