package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which branches to some enclosing label.
 */
public record BranchInsn(Op.Branch op, BranchTarget target) implements Insn<Op.Branch>, Cacheable {
    public BranchInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("target", target);
    }

    @Override
    public void writeTo(WasmOutputStream wos, Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(target));
    }
}
