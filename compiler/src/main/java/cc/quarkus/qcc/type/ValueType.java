package cc.quarkus.qcc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * An "object" in memory, which consists of word types and structured types.
 */
public abstract class ValueType extends Type {
    private static final VarHandle constTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "constType", VarHandle.class, ValueType.class, ValueType.class);
    private static final VarHandle typeTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "typeType", VarHandle.class, ValueType.class, TypeType.class);

    private volatile ValueType constType;
    @SuppressWarnings("unused") // VarHandle
    private volatile TypeType typeType;

    ValueType(final TypeSystem typeSystem, final int hashCode, final boolean const_) {
        super(typeSystem, hashCode * 19 + Boolean.hashCode(const_));
        if (const_) {
            constType = this;
        }
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

    abstract ValueType constructConst();

    public ValueType asConst() {
        ValueType constType = this.constType;
        if (constType != null) {
            return constType;
        }
        ValueType newConstType = constructConst();
        while (! constTypeHandle.compareAndSet(this, null, newConstType)) {
            constType = this.constType;
            if (constType != null) {
                return constType;
            }
        }
        return newConstType;
    }

    public boolean isConst() {
        return this == constType;
    }

    /**
     * Get the {@code TypeType} of this type.  The initial type is not const.
     *
     * @return the type's type type
     */
    public final TypeType getTypeType() {
        TypeType typeType = this.typeType;
        if (typeType != null) {
            return typeType;
        }
        TypeType newTypeType = typeSystem.createTypeType(this);
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
        return this == other || super.equals(other) && isConst() == other.isConst();
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
        if (isConst()) b.append("const ");
        return b;
    }

    public abstract int getAlign();
}
