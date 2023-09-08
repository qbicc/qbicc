package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which operates on two memory instances (destination and source).
 */
public record MemoryAndMemoryInsn(Op.MemoryAndMemory op, Memory destination, Memory source) implements Insn<Op.MemoryAndMemory> {
    public MemoryAndMemoryInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("destination", destination);
        Assert.checkNotNullParam("source", source);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        ev.visit(op, encoder.encode(destination), encoder.encode(source));
    }
}
