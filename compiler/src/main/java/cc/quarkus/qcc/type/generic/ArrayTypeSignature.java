package cc.quarkus.qcc.type.generic;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.type.definition.ClassContext;

/**
 *
 */
public final class ArrayTypeSignature extends ReferenceTypeSignature {
    private final TypeSignature elementTypeSignature;

    ArrayTypeSignature(final TypeSignature elementTypeSignature) {
        super(elementTypeSignature.hashCode() * 19 + ArrayTypeSignature.class.hashCode());
        this.elementTypeSignature = elementTypeSignature;
    }

    public TypeSignature getElementTypeSignature() {
        return elementTypeSignature;
    }

    public boolean equals(final ReferenceTypeSignature other) {
        return other instanceof ArrayTypeSignature && equals((ArrayTypeSignature) other);
    }

    public boolean equals(final ArrayTypeSignature other) {
        return super.equals(other) && elementTypeSignature.equals(other.elementTypeSignature);
    }

    public StringBuilder toString(final StringBuilder target) {
        return elementTypeSignature.toString(target.append('['));
    }

    public static ArrayTypeSignature parse(ClassContext classContext, ByteBuffer buf) {
        expect(buf, '[');
        return Cache.get(classContext).getArrayTypeSignature(TypeSignature.parse(classContext, buf));
    }
}
