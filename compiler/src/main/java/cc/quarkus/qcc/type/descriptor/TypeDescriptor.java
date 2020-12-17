package cc.quarkus.qcc.type.descriptor;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.type.definition.ClassContext;

/**
 * A descriptor which represents the type of a field, a class, or a local variable.
 */
public abstract class TypeDescriptor extends Descriptor {
    TypeDescriptor(final int hashCode) {
        super(hashCode);
    }

    public boolean isClass2() {
        return false;
    }

    public boolean isVoid() {
        return false;
    }

    public final boolean equals(final Descriptor other) {
        return other instanceof TypeDescriptor && equals((TypeDescriptor) other);
    }

    public boolean equals(final TypeDescriptor other) {
        return super.equals(other);
    }

    public static TypeDescriptor parse(final ClassContext classContext, final ByteBuffer buf) {
        int i = peek(buf);
        if (i == '[') {
            return ArrayTypeDescriptor.parse(classContext, buf);
        } else if (i == 'L') {
            return ClassTypeDescriptor.parse(classContext, buf);
        } else {
            return BaseTypeDescriptor.parse(buf);
        }
    }
}
