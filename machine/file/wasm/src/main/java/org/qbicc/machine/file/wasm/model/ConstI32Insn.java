package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which takes a constant value argument.
 */
public record ConstI32Insn(Op.ConstI32 op, int val) implements Insn<Op.ConstI32>, Cacheable {
    public ConstI32Insn {
        Assert.checkNotNullParam("op", op);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.s32(val);
    }
}
