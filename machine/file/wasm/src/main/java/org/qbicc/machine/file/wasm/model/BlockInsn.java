package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.ValType;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

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

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        if (type.parameterTypes().isEmpty()) {
            if (type.resultTypes().isEmpty()) {
                wos.rawByte(0x40);
            } else if (type.resultTypes().size() == 1) {
                wos.type(type.resultTypes().get(0));
            } else {
                wos.u32(encoder.encode(type));
            }
        } else {
            wos.u32(encoder.encode(type));
        }
        body.writeTo(wos, new Encoder.Delegating() {
            @Override
            public Encoder delegate() {
                return encoder;
            }

            @Override
            public int encode(BranchTarget branchTarget) {
                return branchTarget == BlockInsn.this ? 0 : 1 + delegate().encode(branchTarget);
            }
        });
    }
}
