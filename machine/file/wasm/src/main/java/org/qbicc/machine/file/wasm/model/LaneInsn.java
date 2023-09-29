package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 *
 */
public record LaneInsn(Op.Lane op, int laneIdx) implements Insn<Op.Lane>, Cacheable {
    public LaneInsn {
        Assert.checkNotNullParam("op", op);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(laneIdx);
    }
}
