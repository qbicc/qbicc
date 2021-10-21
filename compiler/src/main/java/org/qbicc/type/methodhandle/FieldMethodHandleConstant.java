package org.qbicc.type.methodhandle;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public final class FieldMethodHandleConstant extends MethodHandleConstant {
    private final String fieldName;

    public FieldMethodHandleConstant(final ClassTypeDescriptor owner, final String fieldName, final MethodHandleKind kind) {
        super(fieldName.hashCode(), owner, kind);
        if (! kind.isFieldTarget()) {
            throw new IllegalArgumentException("Field method handle cannot be of kind " + kind);
        }
        this.fieldName = Assert.checkNotNullParam("fieldName", fieldName);
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean equals(final MethodHandleConstant other) {
        return other instanceof FieldMethodHandleConstant && equals((FieldMethodHandleConstant) other);
    }

    public boolean equals(final FieldMethodHandleConstant other) {
        return super.equals(other) && fieldName.equals(other.fieldName);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(getKind()).append('[').append(getOwnerDescriptor()).append(':').append(fieldName).append(']');
    }
}
