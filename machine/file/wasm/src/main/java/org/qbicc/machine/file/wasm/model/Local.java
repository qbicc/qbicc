package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.ValType;

/**
 * A local variable within a function.
 */
public record Local(String name, ValType type, int parameterIndex) implements Named {
    public Local {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
    }

    public Local(String name, ValType type) {
        this(name, type, -1);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
