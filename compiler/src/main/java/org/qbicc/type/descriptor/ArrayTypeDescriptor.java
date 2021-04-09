package org.qbicc.type.descriptor;

import java.nio.ByteBuffer;

import org.qbicc.context.ClassContext;

/**
 * A type descriptor which represents an array type.
 */
public final class ArrayTypeDescriptor extends TypeDescriptor {
    private final TypeDescriptor elementTypeDescriptor;

    ArrayTypeDescriptor(final TypeDescriptor elementTypeDescriptor) {
        super(elementTypeDescriptor.hashCode() * 19 + ArrayTypeDescriptor.class.hashCode());
        this.elementTypeDescriptor = elementTypeDescriptor;
    }

    public TypeDescriptor getElementTypeDescriptor() {
        return elementTypeDescriptor;
    }

    public boolean equals(final TypeDescriptor other) {
        return other instanceof ArrayTypeDescriptor && equals((ArrayTypeDescriptor) other);
    }

    public boolean equals(final ArrayTypeDescriptor other) {
        return super.equals(other) && elementTypeDescriptor.equals(other.elementTypeDescriptor);
    }

    public StringBuilder toString(final StringBuilder target) {
        return elementTypeDescriptor.toString(target.append('['));
    }

    public static ArrayTypeDescriptor parse(ClassContext classContext, ByteBuffer buf) {
        int i = next(buf);
        if (i != '[') {
            throw parseError();
        }
        return of(classContext, TypeDescriptor.parse(classContext, buf));
    }

    public static ArrayTypeDescriptor of(final ClassContext classContext, final TypeDescriptor elementTypeDescriptor) {
        return Cache.get(classContext).getArrayTypeDescriptor(elementTypeDescriptor);
    }
}
