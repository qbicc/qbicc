package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.TagAttribute;

/**
 *
 */
public record DefinedTag(TagAttribute attribute, FuncType type) implements Defined, Tag {
    public DefinedTag {
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("type", type);
    }
}
