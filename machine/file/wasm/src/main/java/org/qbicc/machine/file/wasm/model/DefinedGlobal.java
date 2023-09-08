package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.ValType;

/**
 * A global that is defined directly with an initializer.
 */
public record DefinedGlobal(ValType type, Mutability mutability, InsnSeq init) implements Global, Defined {
    public DefinedGlobal {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("mutability", mutability);
        Assert.checkNotNullParam("init", init);
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
