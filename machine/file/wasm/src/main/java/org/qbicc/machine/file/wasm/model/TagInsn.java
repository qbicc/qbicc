package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which operates on a tag.
 *
 * @param op The operation (not {@code null}).
 * @param tag The tag (not {@code null}).
 */
public record TagInsn(Op.Tag op, Tag tag) implements Insn<Op.Tag>, Cacheable {
    /**
     * Construct a new instance.
     *
     * @param op the operation (must not be {@code null})
     * @param tag the tag (must not be {@code null})
     */
    public TagInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("tag", tag);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(tag));
    }
}
