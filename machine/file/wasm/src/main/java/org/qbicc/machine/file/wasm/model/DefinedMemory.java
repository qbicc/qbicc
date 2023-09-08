package org.qbicc.machine.file.wasm.model;

import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Wasm;

/**
 *
 */
public record DefinedMemory(long minSize, long maxSize, List<ActiveSegment> initializers) implements Defined, Memory {
    public DefinedMemory {
        Assert.checkMinimumParameter("minSize", 0, minSize);
        Assert.checkMaximumParameter("minSize", Wasm.LIMITS_MAXIMUM, minSize);
        Assert.checkMinimumParameter("maxSize", minSize, maxSize);
        Assert.checkMaximumParameter("maxSize", Wasm.LIMITS_MAXIMUM, maxSize);
        Assert.checkNotNullParam("initializers", initializers);
    }

    public DefinedMemory(long minSize, List<ActiveSegment> initializers) {
        this(minSize, Wasm.LIMITS_MAXIMUM, initializers);
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
