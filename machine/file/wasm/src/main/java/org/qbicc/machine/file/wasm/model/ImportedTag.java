package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.TagAttribute;

/**
 * An imported tag.
 *
 * @param moduleName the module name (not {@code null})
 * @param name the import name (not {@code null})
 * @param attribute the tag attribute (not {@code null})
 * @param type the tag type (not {@code null})
 */
public record ImportedTag(String moduleName, String name, TagAttribute attribute, FuncType type) implements Imported, Tag {
    /**
     * Construct a new instance.
     *
     * @param moduleName the module name (must not be {@code null})
     * @param name the import name (must not be {@code null})
     * @param attribute the tag attribute (must not be {@code null})
     * @param type the type of the tag (must not be {@code null})
     */
    public ImportedTag {
        Assert.checkNotNullParam("moduleName", moduleName);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("type", type);
        if (attribute == TagAttribute.EXCEPTION && ! type.resultTypes().isEmpty()) {
            throw new IllegalArgumentException("Result types must be empty for exceptions");
        }
    }
}
