package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.ValType;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * A block instruction which contains a sequence of nested instructions.
 */
public record BlockInsn(Op.Block op, InsnSeq body, FuncType type) implements Insn<Op.Block>, BranchTarget {
    public BlockInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("body", body);
        Assert.checkNotNullParam("type", type);
    }

    public BlockInsn(Op.Block op, InsnSeq body) {
        this(op, body, FuncType.EMPTY);
    }

    public BlockInsn(Op.Block op, InsnSeq body, ValType type) {
        this(op, body, FuncType.returning(Assert.checkNotNullParam("type", type)));
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        if (type.parameterTypes().isEmpty()) {
            if (type.resultTypes().isEmpty()) {
                ev.visit(op());
            } else if (type.resultTypes().size() == 1) {
                ev.visit(op(), type.resultTypes().get(0));
            } else {
                ev.visit(op(), encoder.encode(type));
            }
        } else {
            ev.visit(op(), encoder.encode(type));
        }
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
                return encoder.encode(seg);
            }

            @Override
            public int encode(Tag tag) {
                return encoder.encode(tag);
            }
        });
    }
}
