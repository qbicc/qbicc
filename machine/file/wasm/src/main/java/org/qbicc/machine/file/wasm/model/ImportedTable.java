package org.qbicc.machine.file.wasm.model;

import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.Wasm;

/**
 *
 */
public record ImportedTable(String moduleName, String name, RefType type, long minSize, long maxSize, List<ActiveElement> initializers) implements Imported, Table {

    public ImportedTable {
        Assert.checkNotNullParam("moduleName", moduleName);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        Assert.checkMinimumParameter("minSize", 0, minSize);
        Assert.checkMaximumParameter("minSize", Wasm.LIMITS_MAXIMUM, minSize);
        Assert.checkMinimumParameter("maxSize", minSize, maxSize);
        Assert.checkMaximumParameter("maxSize", Wasm.LIMITS_MAXIMUM, maxSize);
        Assert.checkNotNullParam("initializers", initializers);
    }

    public ImportedTable(String moduleName, String name, RefType type, long minSize, List<ActiveElement> initializers) {
        this(moduleName, name, type, minSize, Wasm.LIMITS_MAXIMUM, initializers);
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
