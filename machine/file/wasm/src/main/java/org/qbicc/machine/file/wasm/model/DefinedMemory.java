package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Wasm;

/**
 *
 */
public record DefinedMemory(long minSize, long maxSize, boolean shared) implements Defined, Memory {
    public DefinedMemory {
        Assert.checkMinimumParameter("minSize", 0, minSize);
        Assert.checkMaximumParameter("minSize", Wasm.LIMITS_MAXIMUM, minSize);
        Assert.checkMinimumParameter("maxSize", minSize, maxSize);
        Assert.checkMaximumParameter("maxSize", Wasm.LIMITS_MAXIMUM, maxSize);
    }

    public DefinedMemory(long minSize, long maxSize) {
        this(minSize, maxSize, false);
    }

    public DefinedMemory(long minSize) {
        this(minSize, Wasm.LIMITS_MAXIMUM);
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
