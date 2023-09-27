package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.ValType;

/**
 * A global that is defined directly with an initializer.
 */
public record DefinedGlobal(String name, ValType type, Mutability mutability, InsnSeq init) implements Global, Defined {
    public DefinedGlobal {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("mutability", mutability);
        Assert.checkNotNullParam("init", init);
    }
}
