package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.RefType;

/**
 *
 */
public record DeclarativeElement(RefType type, ElementInit init) implements Element {
    public DeclarativeElement {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("init", init);
    }
}
