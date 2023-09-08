package org.qbicc.machine.file.wasm.model;

import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction which targets some enclosing label.
 */
public record MultiBranchInsn(Op.MultiBranch op, List<BranchTarget> branchTargets, BranchTarget defaultTarget) implements Insn<Op.MultiBranch> {
    public MultiBranchInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("branchTargets", branchTargets);
        Assert.checkNotNullParam("defaultTarget", defaultTarget);
    }

    @Override
    public <E extends Exception> void accept(InsnSeqVisitor<E> ev, Encoder encoder) throws E {
        int size = branchTargets.size();
        int[] resolved = new int[size];
        for (int i = 0; i < size; i++) {
            resolved[i] = encoder.encode(branchTargets.get(i));
        }
        ev.visit(op, encoder.encode(defaultTarget), resolved);
    }
}
