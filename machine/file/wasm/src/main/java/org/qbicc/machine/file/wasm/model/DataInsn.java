package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 *
 */
public record DataInsn(Op.Data op, SegmentHandle segmentHandle) implements Insn<Op.Data> {
    public DataInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("segmentHandle", segmentHandle);
    }

    public DataInsn(Op.Data op, Segment segment) {
        this(op, SegmentHandle.of(segment));
    }

    public Segment segment() {
        return segmentHandle.segment();
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(segment()));
    }
}
