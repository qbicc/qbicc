package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.Wasm;

/**
 *
 */
public record ImportedTable(String moduleName, String name, RefType type, long minSize, long maxSize, boolean shared) implements Imported, Table {

    public ImportedTable {
        Assert.checkNotNullParam("moduleName", moduleName);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        Assert.checkMinimumParameter("minSize", 0, minSize);
        Assert.checkMaximumParameter("minSize", Wasm.LIMITS_MAXIMUM, minSize);
        Assert.checkMinimumParameter("maxSize", minSize, maxSize);
        Assert.checkMaximumParameter("maxSize", Wasm.LIMITS_MAXIMUM, maxSize);
    }

    public ImportedTable(String moduleName, String name, RefType type, long minSize, long maxSize) {
        this(moduleName, name, type, minSize, maxSize, false);
    }

    public ImportedTable(String moduleName, String name, RefType type, long minSize) {
        this(moduleName, name, type, minSize, Wasm.LIMITS_MAXIMUM);
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
