package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which takes an element as its argument.
 */
public record ElementInsn(Op.Element op, ElementHandle elementHandle) implements Insn<Op.Element> {
    public ElementInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("elementHandle", elementHandle);
    }

    public ElementInsn(Op.Element op, Element element) {
        this(op, ElementHandle.of(element));
    }

    public Element element() {
        return elementHandle().element();
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(element()));
    }
}
