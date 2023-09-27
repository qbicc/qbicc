package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which operates on a table.
 */
public record TableAndFuncTypeInsn(Op.TableAndFuncType op, Table table, FuncType type) implements Insn<Op.TableAndFuncType> {
    public TableAndFuncTypeInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("table", table);
        Assert.checkNotNullParam("type", type);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(type()));
        wos.u32(encoder.encode(table()));
    }
}
