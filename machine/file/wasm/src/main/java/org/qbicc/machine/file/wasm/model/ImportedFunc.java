package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;

/**
 *
 */
public record ImportedFunc(String moduleName, String name, FuncType type) implements Imported, Func {
    public ImportedFunc {
        Assert.checkNotNullParam("moduleName", moduleName);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
    }
}
