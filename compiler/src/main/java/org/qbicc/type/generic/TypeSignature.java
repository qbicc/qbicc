package org.qbicc.type.generic;

import java.nio.ByteBuffer;
import java.util.List;

import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public abstract class TypeSignature extends Signature {
    private TypeDescriptor descriptor;

    TypeSignature(final int hashCode) {
        super(hashCode);
    }

    public final boolean equals(final Signature other) {
        return other instanceof TypeSignature && equals((TypeSignature) other);
    }

    public boolean equals(final TypeSignature other) {
        return super.equals(other);
    }

    public TypeDescriptor asDescriptor(ClassContext classContext) {
        TypeDescriptor descriptor = this.descriptor;
        if (descriptor == null) {
            descriptor = this.descriptor = makeDescriptor(classContext);
        }
        return descriptor;
    }

    abstract TypeDescriptor makeDescriptor(ClassContext classContext);

    public static TypeSignature parse(ClassContext classContext, ByteBuffer buf) {
        int i = peek(buf);
        if (i == 'L') {
            return ClassTypeSignature.parse(classContext, buf);
        } else if (i == 'T') {
            return TypeVariableSignature.parse(classContext, buf);
        } else if (i == '[') {
            return ArrayTypeSignature.parse(classContext, buf);
        } else {
            return BaseTypeSignature.parse(buf);
        }
    }

    public static TypeSignature synthesize(ClassContext classContext, TypeDescriptor descriptor) {
        if (descriptor instanceof BaseTypeDescriptor) {
            return BaseTypeSignature.forChar(((BaseTypeDescriptor) descriptor).getShortName());
        } else if (descriptor instanceof ArrayTypeDescriptor) {
            return Cache.get(classContext).getArrayTypeSignature(synthesize(classContext, ((ArrayTypeDescriptor) descriptor).getElementTypeDescriptor()));
        } else {
            assert descriptor instanceof ClassTypeDescriptor;
            ClassTypeDescriptor classDesc = (ClassTypeDescriptor) descriptor;
            return Cache.get(classContext).getTopLevelTypeSignature(classDesc.getPackageName(), classDesc.getClassName(), List.of());
        }
    }
}
