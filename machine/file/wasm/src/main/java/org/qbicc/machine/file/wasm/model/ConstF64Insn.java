package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which takes a constant value argument.
 */
public record ConstF64Insn(Op.ConstF64 op, double val) implements Insn<Op.ConstF64> {
    public ConstF64Insn {
        Assert.checkNotNullParam("op", op);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.f64(val);
    }
}
