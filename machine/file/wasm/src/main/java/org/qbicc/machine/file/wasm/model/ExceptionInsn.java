package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 *
 */
public record ExceptionInsn(Op.Exception op, BranchTarget target) implements Insn<Op.Exception> {
    public ExceptionInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("target", target);
        if (target instanceof BlockInsn bi && bi.op() != Op.Block.try_) {
            throw new IllegalArgumentException("Exception instruction must target a `try` block");
        }
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(target));
    }
}
