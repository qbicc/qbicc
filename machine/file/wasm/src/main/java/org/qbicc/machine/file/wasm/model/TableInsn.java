package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which operates on a table.
 */
public record TableInsn(Op.Table op, Table table) implements Insn<Op.Table> {
    public TableInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("table", table);
    }

    @Override
    public void writeTo(WasmOutputStream wos, Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(table()));
    }
}
