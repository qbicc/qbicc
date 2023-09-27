package org.qbicc.machine.file.wasm.model;

import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.RefType;

/**
 *
 */
public record PassiveElement(String name, RefType type, List<InsnSeq> init) implements Element {
    public PassiveElement {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("init", init);
    }
}
