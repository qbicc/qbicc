package cc.quarkus.qcc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;

/**
 * A value type which refers to something that can be referred to by {@linkplain ReferenceType reference}.
 */
public abstract class ObjectType extends ValueType {
    private static final VarHandle referenceTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "referenceType", VarHandle.class, ObjectType.class, ReferenceType.class);

    @SuppressWarnings("unused")
    private volatile ReferenceType referenceType;

    ObjectType(final TypeSystem typeSystem, final int hashCode) {
        super(typeSystem, hashCode);
    }

    public DefinedTypeDefinition getDefinition() {
        throw new IllegalArgumentException("Type " + this + " is not a defined type");
    }

    /**
     * Get a reference to this type.  The initial reference is not nullable and not const.
     *
     * @return the type's reference type
     */
    public final ReferenceType getReference() {
        ReferenceType referenceType = this.referenceType;
        if (referenceType != null) {
            return referenceType;
        }
        ReferenceType newReferenceType = createReferenceType();
        while (! referenceTypeHandle.compareAndSet(this, null, newReferenceType)) {
            referenceType = this.referenceType;
            if (referenceType != null) {
                return referenceType;
            }
        }
        return newReferenceType;
    }

    abstract ReferenceType createReferenceType();

    public abstract boolean hasSuperClass();

    public ClassObjectType getSuperClassType() {
        return null;
    }

    public boolean isComplete() {
        return false;
    }

    public abstract boolean isSubtypeOf(ObjectType other);

    public abstract ObjectType getCommonSupertype(ObjectType other);

    public boolean isSupertypeOf(ObjectType other) {
        return other.isSubtypeOf(this);
    }
}
