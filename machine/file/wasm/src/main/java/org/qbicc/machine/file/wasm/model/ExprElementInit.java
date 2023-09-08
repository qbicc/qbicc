package org.qbicc.machine.file.wasm.model;

import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 * An element initializer which uses a list of expressions to initialize each element.
 */
public record ExprElementInit(List<InsnSeq> items) implements ElementInit {
    public ExprElementInit {
        Assert.checkNotNullParam("items", items);
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
