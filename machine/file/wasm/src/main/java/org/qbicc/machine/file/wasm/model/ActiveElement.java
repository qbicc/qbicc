package org.qbicc.machine.file.wasm.model;

import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.RefType;

/**
 *
 */
public record ActiveElement(String name, InsnSeq offset, RefType type, List<InsnSeq> init) implements Element {
    public ActiveElement {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("offset", offset);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("init", init);
    }
}
