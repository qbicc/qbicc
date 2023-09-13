package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which operates on a tag.
 *
 * @param op The operation (not {@code null}).
 * @param tag The tag (not {@code null}).
 */
public record TagInsn(Op.Tag op, Tag tag) implements Insn<Op.Tag> {
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

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(tag));
    }
}
