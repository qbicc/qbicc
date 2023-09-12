package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.RefType;

/**
 *
 */
public record ActiveElement(InsnSeq offset, RefType type, ElementInit init) implements Element {
    public ActiveElement {
        Assert.checkNotNullParam("offset", offset);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("init", init);
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
