package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which operates on an element and a table.
 */
public record ElementAndTableInsn(Op.ElementAndTable op, ElementHandle elementHandle, Table table) implements Insn<Op.ElementAndTable> {
    public ElementAndTableInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("elementHandle", elementHandle);
        Assert.checkNotNullParam("table", table);
    }

    public ElementAndTableInsn(Op.ElementAndTable op, Element element, Table table) {
        this(op, ElementHandle.of(element), table);
    }

    public Element element() {
        return elementHandle().element();
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(element()), encoder.encode(table));
    }
}
