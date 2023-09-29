package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which operates on a local variable.
 *
 * @param op The operation (not {@code null}).
 * @param local The local variable (not {@code null}).
 */
public record LocalInsn(Op.Local op, Local local) implements Insn<Op.Local>, Cacheable {
    /**
     * Construct a new instance.
     *
     * @param op the operation (must not be {@code null})
     * @param local the local variable (must not be {@code null})
     */
    public LocalInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("local", local);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(local));
    }
}
