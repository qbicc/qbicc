package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which operates on a table.
 */
public record TableAndFuncTypeInsn(Op.TableAndFuncType op, Table table, FuncType type) implements Insn<Op.TableAndFuncType> {
    public TableAndFuncTypeInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("table", table);
        Assert.checkNotNullParam("type", type);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(table), encoder.encode(type));
    }
}
