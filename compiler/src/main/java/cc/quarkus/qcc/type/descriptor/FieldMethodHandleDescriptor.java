package org.qbicc.type.descriptor;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class FieldMethodHandleDescriptor extends MethodHandleDescriptor {
    private final String fieldName;

    public FieldMethodHandleDescriptor(final ClassTypeDescriptor owner, final String fieldName, final MethodHandleKind kind) {
        super(fieldName.hashCode(), owner, kind);
        if (! kind.isFieldTarget()) {
            throw new IllegalArgumentException("Field method handle cannot be of kind " + kind);
        }
        this.fieldName = Assert.checkNotNullParam("fieldName", fieldName);
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean equals(final MethodHandleDescriptor other) {
        return other instanceof FieldMethodHandleDescriptor && equals((FieldMethodHandleDescriptor) other);
    }

    public boolean equals(final FieldMethodHandleDescriptor other) {
        return super.equals(other) && fieldName.equals(other.fieldName);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(getKind()).append('[').append(getOwnerDescriptor()).append(':').append(fieldName).append(']');
    }
}
