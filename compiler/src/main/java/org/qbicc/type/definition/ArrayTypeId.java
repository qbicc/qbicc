package org.qbicc.type.definition;

/**
 * A type ID for an array class.
 */
public final class ArrayTypeId extends TypeId {
    private final TypeId elementTypeId;
    private final int dimensions;

    ArrayTypeId(TypeId elementTypeId) {
        this.elementTypeId = elementTypeId;
        dimensions = elementTypeId instanceof ArrayTypeId next ? next.dimensions + 1 : 1;
    }

    /**
     * Get the type ID of elements of this array.
     *
     * @return the element type ID (not {@code null})
     */
    public TypeId getElementTypeId() {
        return elementTypeId;
    }

    /**
     * Get the dimensions of this array type (the number of array types, including this one, before a non-array type is found).
     *
     * @return the dimensions
     */
    public int getDimensions() {
        return dimensions;
    }
}
