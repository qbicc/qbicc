package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 *
 */
public record LaneInsn(Op.Lane op, int laneIdx) implements Insn<Op.Lane> {
    public LaneInsn {
        Assert.checkNotNullParam("op", op);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, laneIdx);
    }
}
