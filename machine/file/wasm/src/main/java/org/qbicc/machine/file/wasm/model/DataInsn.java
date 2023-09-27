package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 *
 */
public record DataInsn(Op.Data op, Segment segment) implements Insn<Op.Data> {
    public DataInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("segment", segment);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(segment()));
    }
}
