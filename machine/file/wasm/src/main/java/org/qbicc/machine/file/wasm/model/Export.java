package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;

/**
 * An export of an item under a given name.
 */
public record Export<E extends Exportable>(String name, E exported) {
    public Export {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("exported", exported);
    }
}
