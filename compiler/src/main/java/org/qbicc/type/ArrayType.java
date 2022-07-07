package org.qbicc.type;

/**
 * A fixed-size value array type.
 */
public final class ArrayType extends ValueType {
    private final ValueType elementType;
    private final long elementCount;
    private final long elementSize;

    ArrayType(final TypeSystem typeSystem, final ValueType elementType, final long elementCount) {
        super(typeSystem, (int) (elementType.hashCode() * 19 + elementCount));
        this.elementType = elementType;
        this.elementCount = elementCount;
        this.elementSize = TypeUtil.alignUp(elementType.getSize(), elementType.getAlign());
    }

    public long getSize() {
        return Math.multiplyExact(elementCount, elementSize);
    }

    public ValueType getElementType() {
        return elementType;
    }

    public <T extends ValueType> T getElementType(Class<T> expected) {
        return expected.cast(getElementType());
    }

    public long getElementCount() {
        return elementCount;
    }

    public long getElementSize() {
        return elementSize;
    }

    public int getAlign() {
        return elementType.getAlign();
    }

    public PointerType asEquivalentPointerType() {
        return getElementType().getPointer();
    }

    public boolean equals(final ValueType other) {
        return other instanceof ArrayType && equals((ArrayType) other);
    }

    @Override
    public ValueType getTypeAtOffset(long offset) {
        if (0 <= offset && offset < getSize()) {
            return elementType.getTypeAtOffset(offset % elementSize);
        } else {
            return getTypeSystem().getVoidType();
        }
    }

    public boolean equals(final ArrayType other) {
        return this == other || super.equals(other) && elementCount == other.elementCount && elementType.equals(other.elementType);
    }

    public StringBuilder toString(final StringBuilder b) {
        return elementType.toString(super.toString(b).append("array [").append(elementCount).append("] of "));
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return elementType.toFriendlyString(b).append('[').append(elementCount).append(']');
    }
}
