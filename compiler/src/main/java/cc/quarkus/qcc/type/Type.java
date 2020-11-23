package cc.quarkus.qcc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A type.
 */
public abstract class Type {
    public static final Type[] NO_TYPES = new Type[0];

    private static final VarHandle pointerTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "pointerType", VarHandle.class, Type.class, PointerType.class);

    final TypeSystem typeSystem;
    private final int hashCode;
    private volatile PointerType pointerType;

    Type(final TypeSystem typeSystem, final int hashCode) {
        this.typeSystem = typeSystem;
        this.hashCode = hashCode;
    }

    /**
     * Get the type system which owns this type.
     *
     * @return the type system which owns this type
     */
    public final TypeSystem getTypeSystem() {
        return typeSystem;
    }

    /**
     * Get the pointer type to this type.
     *
     * @return the pointer type
     */
    public final PointerType getPointer() {
        PointerType pointerType = this.pointerType;
        if (pointerType != null) {
            return pointerType;
        }
        PointerType newPointerType = typeSystem.createPointer(this);
        while (! pointerTypeHandle.compareAndSet(this, null, newPointerType)) {
            pointerType = this.pointerType;
            if (pointerType != null) {
                return pointerType;
            }
        }
        return newPointerType;
    }

    public boolean isImplicitlyConvertibleFrom(Type other) {
        return equals(other);
    }

    public boolean isCompatibleWith(Type other) {
        return equals(other);
    }

    /**
     * Determine whether variables of this type are writable.
     *
     * @return {@code true} if the variable of this type would be writable, {@code false} otherwise
     */
    public boolean isWritable() {
        return ! isConst();
    }

    /**
     * Determine whether this type is complete (that is, it has a size and can be instantiated).
     *
     * @return {@code true} if the type is complete, or {@code false} if it is incomplete
     */
    public boolean isComplete() {
        return false;
    }

    /**
     * Determine whether this type is {@code const}-qualified.
     *
     * @return {@code true} if this type is {@code const}-qualified, {@code false} otherwise
     */
    public boolean isConst() {
        return false;
    }

    public Type getConstraintType() {
        return typeSystem.getVoidType();
    }

    public final int hashCode() {
        return hashCode;
    }

    public final boolean equals(final Object obj) {
        return obj instanceof Type && equals((Type) obj);
    }

    public boolean equals(final Type other) {
        return other == this || other != null && typeSystem == other.typeSystem && hashCode == other.hashCode;
    }

    public abstract StringBuilder toString(StringBuilder b);

    public final String toString() {
        return toString(new StringBuilder()).toString();
    }
}
