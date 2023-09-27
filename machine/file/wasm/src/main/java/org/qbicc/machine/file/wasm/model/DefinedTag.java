package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.TagAttribute;

/**
 *
 */
public record DefinedTag(String name, TagAttribute attribute, FuncType type) implements Defined, Tag {
    public DefinedTag {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("type", type);
        if (attribute == TagAttribute.EXCEPTION && ! type.resultTypes().isEmpty()) {
            throw new IllegalArgumentException("Result types must be empty for exceptions");
        }
    }
}
