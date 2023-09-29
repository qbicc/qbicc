package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which takes a constant value argument.
 */
public record ConstF32Insn(Op.ConstF32 op, float val) implements Insn<Op.ConstF32>, Cacheable {
    public ConstF32Insn {
        Assert.checkNotNullParam("op", op);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.f32(val);
    }
}
