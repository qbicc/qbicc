package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.Wasm;

/**
 *
 */
public record DefinedTable(RefType type, long minSize, long maxSize, boolean shared) implements Defined, Table {

    public DefinedTable {
        Assert.checkNotNullParam("type", type);
        Assert.checkMinimumParameter("minSize", 0, minSize);
        Assert.checkMaximumParameter("minSize", Wasm.LIMITS_MAXIMUM, minSize);
        Assert.checkMinimumParameter("maxSize", minSize, maxSize);
        Assert.checkMaximumParameter("maxSize", Wasm.LIMITS_MAXIMUM, maxSize);
    }

    public DefinedTable(RefType type, long minSize, long maxSize) {
        this(type, minSize, maxSize, false);
    }

    public DefinedTable(RefType type, long minSize) {
        this(type, minSize, Wasm.LIMITS_MAXIMUM);
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
