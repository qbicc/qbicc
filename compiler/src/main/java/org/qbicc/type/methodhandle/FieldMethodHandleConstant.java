package org.qbicc.type.methodhandle;

import java.util.Objects;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public final class FieldMethodHandleConstant extends MethodHandleConstant {
    private final String fieldName;
    private final TypeDescriptor descriptor;

    public FieldMethodHandleConstant(final ClassTypeDescriptor owner, final String fieldName, final MethodHandleKind kind, TypeDescriptor descriptor) {
        super(Objects.hash(fieldName, descriptor), owner, kind);
        if (! kind.isFieldTarget()) {
            throw new IllegalArgumentException("Field method handle cannot be of kind " + kind);
        }
        this.descriptor = Assert.checkNotNullParam("descriptor", descriptor);
        this.fieldName = Assert.checkNotNullParam("fieldName", fieldName);
    }

    public String getFieldName() {
        return fieldName;
    }

    public TypeDescriptor getDescriptor() {
        return descriptor;
    }

    public boolean equals(final MethodHandleConstant other) {
        return other instanceof FieldMethodHandleConstant && equals((FieldMethodHandleConstant) other);
    }

    public boolean equals(final FieldMethodHandleConstant other) {
        return super.equals(other) && fieldName.equals(other.fieldName) && descriptor.equals(other.descriptor);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(getKind()).append('[').append(getOwnerDescriptor()).append(':').append(fieldName).append(']');
    }
}
