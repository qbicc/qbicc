package org.qbicc.machine.file.wasm.model;

import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public record FuncListElementInit(List<Func> items) implements ElementInit {
    public FuncListElementInit {
        Assert.checkNotNullParam("items", items);
    }
}
