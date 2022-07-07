package org.qbicc.type;

import java.util.Set;

/**
 * An object type that can be allocated on the heap.
 */
public abstract class PhysicalObjectType extends ObjectType {
    PhysicalObjectType(final TypeSystem typeSystem, final int hashCode) {
        super(typeSystem, hashCode);
    }

    ReferenceType createReferenceType() {
        return typeSystem.createReference(this, Set.of());
    }

    public boolean isComplete() {
        return true;
    }

    /**
     * Get the physical size of this object.  The size does not include object headers or
     * allocator metadata for heap allocation.  The size is not available until after object
     * layout has been completed.  For arrays, the size will not include the array elements;
     * add the element size times the element count to get the final in-memory size.
     *
     * @return the size of the physical object
     * @throws IllegalStateException if object layout has not yet taken place
     */
    public abstract long getSize() throws IllegalStateException;

    @Override
    public ValueType getTypeAtOffset(final long offset) {
        throw new IllegalStateException("Cannot probe type at offset of object type until layout is complete");
    }

    public int getAlign() {
        return typeSystem.getPointerAlignment();
    }

    /**
     * Get the superclass of this type.  All physical object types have a superclass, though
     * in the case of {@code java.lang.Object} it will be {@code null}.
     *
     * @return the superclass of this type
     */
    public abstract ClassObjectType getSuperClassType();

    public boolean hasSuperClassType() {
        return getSuperClassType() != null;
    }
}
