package org.qbicc.type;

/**
 *
 */
public abstract class ScalarType extends ValueType {
    ScalarType(final TypeSystem typeSystem, final int hashCode) {
        super(typeSystem, hashCode);
    }

    public ValueType getTypeAtOffset(final long offset) {
        return offset == 0 ? this : getTypeSystem().getVoidType();
    }
}
