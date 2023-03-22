package org.qbicc.type.definition;

import io.smallrye.common.constraint.Assert;

/**
 * The unique identifier for a non-array class.
 */
public final class LeafTypeId extends TypeId {
    private final DefinedTypeDefinition definition;
    private volatile int typeIdValue = -1;

    LeafTypeId(DefinedTypeDefinition definition) {
        this.definition = definition;
    }

    public boolean hasTypeIdValue() {
        return typeIdValue != -1;
    }

    public int getTypeIdValue() {
        if (typeIdValue == -1) {
            throw new IllegalStateException("Type ID value not assigned");
        }
        return typeIdValue;
    }

    public int getTypeIdValueElse(int elseVal) {
        final int typeIdValue = this.typeIdValue;
        return typeIdValue == -1 ? elseVal : typeIdValue;
    }

    public void setTypeIdValue(int typeIdValue) {
        Assert.checkMinimumParameter("typeIdValue", 0, typeIdValue);
        if (this.typeIdValue != -1) {
            throw new IllegalStateException("Type ID already assigned");
        }
        this.typeIdValue = typeIdValue;
    }

    public DefinedTypeDefinition getTypeDefinition() {
        return definition;
    }
}
