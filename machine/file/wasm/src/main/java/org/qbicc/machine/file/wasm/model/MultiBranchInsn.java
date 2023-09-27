package org.qbicc.machine.file.wasm.model;

import java.io.IOException;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which targets some enclosing label.
 */
public record MultiBranchInsn(Op.MultiBranch op, List<BranchTarget> branchTargets, BranchTarget defaultTarget) implements Insn<Op.MultiBranch> {
    public MultiBranchInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("branchTargets", branchTargets);
        Assert.checkNotNullParam("defaultTarget", defaultTarget);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        int size = branchTargets.size();
        wos.u32(size);
        for (int i = 0; i < size; i++) {
            wos.u32(encoder.encode(branchTargets.get(i)));
        }
        wos.u32(encoder.encode(defaultTarget));
    }
}
