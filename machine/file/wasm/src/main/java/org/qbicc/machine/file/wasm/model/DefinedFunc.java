package org.qbicc.machine.file.wasm.model;

import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.ValType;

/**
 * A function defined in its module.
 */
public record DefinedFunc(FuncType type, List<ValType> localTypes, InsnSeq body) implements Func, Defined, BranchTarget {
    public DefinedFunc {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("localTypes", localTypes);
        Assert.checkNotNullParam("body", body);
    }

    public DefinedFunc(FuncType type, List<ValType> localTypes) {
        this(type, localTypes, new InsnSeq());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
