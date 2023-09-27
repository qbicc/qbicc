package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which operates on a global variable.
 */
public record GlobalInsn(Op.Global op, Global global) implements Insn<Op.Global> {
    public GlobalInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("global", global);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(global()));
    }
}
