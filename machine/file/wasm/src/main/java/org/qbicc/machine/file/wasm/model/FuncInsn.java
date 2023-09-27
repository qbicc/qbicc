package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which takes a function as its argument.
 */
public record FuncInsn(Op.Func op, Func func) implements Insn<Op.Func> {
    public FuncInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("func", func);
    }

    @Override
    public void writeTo(WasmOutputStream wos, Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(func()));
    }
}
