package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which operates on an element and a table.
 */
public record ElementAndTableInsn(Op.ElementAndTable op, Element element, Table table) implements Insn<Op.ElementAndTable>, Cacheable {
    public ElementAndTableInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("element", element);
        Assert.checkNotNullParam("table", table);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(element()));
        wos.u32(encoder.encode(table()));
    }
}
