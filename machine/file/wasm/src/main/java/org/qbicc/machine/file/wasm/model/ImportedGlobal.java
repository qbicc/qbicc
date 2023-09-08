package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.ValType;

/**
 * A global variable which was imported from another module.
 */
public record ImportedGlobal(String moduleName, String name, ValType type, Mutability mutability) implements Global, Imported {
    public ImportedGlobal {
        Assert.checkNotNullParam("moduleName", moduleName);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("mutability", mutability);
    }
}
