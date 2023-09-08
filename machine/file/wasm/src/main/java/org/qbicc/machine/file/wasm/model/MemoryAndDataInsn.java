package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which operates on some memory instance and a data segment.
 */
public record MemoryAndDataInsn(Op.MemoryAndData op, Memory memory, SegmentHandle segmentHandle) implements Insn<Op.MemoryAndData> {
    public MemoryAndDataInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("memory", memory);
        Assert.checkNotNullParam("segmentHandle", segmentHandle);
    }

    public MemoryAndDataInsn(Op.MemoryAndData op, Memory memory, Segment segment) {
        this(op, memory, SegmentHandle.of(segment));
    }

    public Segment segment() {
        return segmentHandle.segment();
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(segment()), encoder.encode(memory));
    }
}
