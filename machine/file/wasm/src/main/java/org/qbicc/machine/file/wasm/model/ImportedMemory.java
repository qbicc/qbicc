package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Wasm;

/**
 *
 */
public record ImportedMemory(String moduleName, String name, long minSize, long maxSize, boolean shared) implements Imported, Memory {
    public ImportedMemory {
        Assert.checkNotNullParam("moduleName", moduleName);
        Assert.checkNotNullParam("name", name);
        Assert.checkMinimumParameter("minSize", 0, minSize);
        Assert.checkMaximumParameter("minSize", Wasm.LIMITS_MAXIMUM, minSize);
        Assert.checkMinimumParameter("maxSize", minSize, maxSize);
        Assert.checkMaximumParameter("maxSize", Wasm.LIMITS_MAXIMUM, maxSize);
    }

    public ImportedMemory(String moduleName, String name, long minSize, long maxSize) {
        this(moduleName, name, minSize, maxSize, false);
    }

    public ImportedMemory(String moduleName, String name, long minSize) {
        this(moduleName, name, minSize, Wasm.LIMITS_MAXIMUM);
    }
}
