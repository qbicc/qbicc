package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * A block instruction which contains a sequence of nested instructions.
 */
public record BlockInsn(Op.Block op, InsnSeq body) implements Insn<Op.Block>, BranchTarget {
    // todo: type..?
    public BlockInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("body", body);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        // todo: types or whatever
        ev.visit(op());
        body.accept(ev, new Encoder() {
            @Override
            public int encode(BranchTarget branchTarget) {
                return branchTarget == BlockInsn.this ? 0 : 1 + encoder.encode(branchTarget);
            }

            @Override
            public int encode(Element element) {
                return encoder.encode(element);
            }

            @Override
            public int encode(Func func) {
                return encoder.encode(func);
            }

            @Override
            public int encode(FuncType type) {
                return encoder.encode(type);
            }

            @Override
            public int encode(Global global) {
                return encoder.encode(global);
            }

            @Override
            public int encode(Memory memory) {
                return encoder.encode(memory);
            }

            @Override
            public int encode(Table table) {
                return encoder.encode(table);
            }

            @Override
            public int encode(Segment seg) {
                return 0;
            }
        });
    }
}
