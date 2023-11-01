package org.qbicc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.ImmutableIntObjectMap;

/**
 * A type.
 */
public abstract class Type {
    public static final Type[] NO_TYPES = new Type[0];

    private static final VarHandle pointerTypesHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "pointerTypes", VarHandle.class, Type.class, ImmutableIntObjectMap.class);

    final TypeSystem typeSystem;
    private final int hashCode;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile ImmutableIntObjectMap<PointerType> pointerTypes = IntObjectMaps.immutable.empty();

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

    @SuppressWarnings("unchecked")
    private ImmutableIntObjectMap<PointerType> caxPointerTypes(ImmutableIntObjectMap<PointerType> expect, ImmutableIntObjectMap<PointerType> update) {
        return (ImmutableIntObjectMap<PointerType>) pointerTypesHandle.compareAndExchange(this, expect, update);
    }

    /**
     * {@return a pointer to this type in the given address space}
     * @param addrSpace the address space
     */
    public final PointerType pointer(int addrSpace) {
        ImmutableIntObjectMap<PointerType> map = this.pointerTypes;
        PointerType pointerType = map.get(addrSpace);
        if (pointerType != null) {
            return pointerType;
        }
        PointerType newPointerType = typeSystem.createPointer((ValueType) this, addrSpace);
        ImmutableIntObjectMap<PointerType> newMap = map.newWithKeyValue(addrSpace, newPointerType);
        ImmutableIntObjectMap<PointerType> witness = caxPointerTypes(map, newMap);
        while (witness != map) {
            map = witness;
            pointerType = map.get(addrSpace);
            if (pointerType != null) {
                return pointerType;
            }
            newMap = map.newWithKeyValue(addrSpace, newPointerType);
            witness = caxPointerTypes(map, newMap);
        }
        return newPointerType;
    }

    /**
     * Get the pointer type to this type.
     *
     * @return the pointer type
     */
    public final PointerType getPointer() {
        return pointer(0);
    }

    public boolean isImplicitlyConvertibleFrom(Type other) {
        return equals(other);
    }

    /**
     * Determine whether this type is complete (that is, it has a size and can be instantiated).
     *
     * @return {@code true} if the type is complete, or {@code false} if it is incomplete
     */
    public boolean isComplete() {
        return false;
    }

    public Type getConstraintType() {
        return typeSystem.getVoidType();
    }

    public int hashCode() {
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

    public final String toFriendlyString() {
        return toFriendlyString(new StringBuilder()).toString();
    }

    public abstract StringBuilder toFriendlyString(final StringBuilder b);
}
