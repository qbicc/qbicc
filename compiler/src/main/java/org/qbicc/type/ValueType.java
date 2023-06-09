package org.qbicc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * An "object" in memory, which consists of word types and structured types.
 */
public abstract class ValueType extends Type {
    private static final VarHandle typeTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "typeType", VarHandle.class, ValueType.class, TypeIdType.class);

    @SuppressWarnings("unused") // VarHandle
    private volatile TypeIdType typeType;

    ValueType(final TypeSystem typeSystem, final int hashCode) {
        super(typeSystem, hashCode);
    }

    /**
     * Get the size of this type, in "bytes" as defined by {@linkplain TypeSystem#getByteBits() the platform byte size}.
     *
     * @return the size
     */
    public abstract long getSize();

    public boolean isComplete() {
        return true;
    }

    /**
     * Get the {@code TypeType} of this type.  The initial type is not const.
     *
     * @return the type's type type
     */
    public final TypeIdType getTypeType() {
        TypeIdType typeType = this.typeType;
        if (typeType != null) {
            return typeType;
        }
        TypeIdType newTypeType = typeSystem.createTypeType(this);
        while (! typeTypeHandle.compareAndSet(this, null, newTypeType)) {
            typeType = this.typeType;
            if (typeType != null) {
                return typeType;
            }
        }
        return newTypeType;
    }

    public final boolean equals(final Type other) {
        return other instanceof ValueType && equals((ValueType) other);
    }

    public boolean equals(final ValueType other) {
        return super.equals(other);
    }

    public ValueType getTypeAtOffset(long offset) {
        return getTypeSystem().getVoidType();
    }

    /**
     * Find the "join" of two types.  The returned type will represent a type that value of either type can be
     * implicitly converted to (i.e. a common supertype), one way or another.  If no join is possible, the poison
     * type is returned.
     *
     * @param other the other type (must not be {@code null})
     * @return the meet type (not {@code null})
     */
    public ValueType join(final ValueType other) {
        return equals(other) ? this : getTypeSystem().getPoisonType();
    }

    public StringBuilder toString(final StringBuilder b) {
        if (! isComplete()) b.append("incomplete ");
        return b;
    }

    public abstract int getAlign();
}
