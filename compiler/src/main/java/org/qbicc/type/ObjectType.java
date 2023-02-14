package org.qbicc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Base64;

import org.qbicc.type.definition.DefinedTypeDefinition;

/**
 * A value type which refers to something that can be referred to by {@linkplain ReferenceType reference}.
 */
public abstract class ObjectType extends ValueType {
    private static final VarHandle referenceTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "referenceType", VarHandle.class, ObjectType.class, ReferenceType.class);
    private static final VarHandle refArrayTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "refArrayType", VarHandle.class, ObjectType.class, ReferenceArrayObjectType.class);


    @SuppressWarnings("unused")
    private volatile ReferenceType referenceType;
    @SuppressWarnings("unused")
    private volatile ReferenceArrayObjectType refArrayType;

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

    /**
     * Get the referenceArrayObjectType with this type as its element.
     *
     * @return the referenceArrayObjectType
     */
    public final ReferenceArrayObjectType getReferenceArrayObject() {
        ReferenceArrayObjectType refArrayType = this.refArrayType;
        if (refArrayType != null) {
            return refArrayType;
        }
        ReferenceArrayObjectType newReferenceArrayObjectType = typeSystem.createReferenceArrayObject(this);
        while (! refArrayTypeHandle.compareAndSet(this, null, newReferenceArrayObjectType)) {
            refArrayType = this.refArrayType;
            if (refArrayType != null) {
                return refArrayType;
            }
        }
        return newReferenceArrayObjectType;
    }

    @Override
    public ValueType getTypeAtOffset(long offset) {
        return getTypeSystem().getVoidType();
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

    @Override
    public final boolean equals(ValueType other) {
        return other instanceof ObjectType && equals((ObjectType) other);
    }

    public boolean equals(ObjectType other) {
        return super.equals(other);
    }

    static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public StringBuilder toFriendlyString(final StringBuilder b) {
        // override in subclasses that do not have a definition
        DefinedTypeDefinition definition = getDefinition();
        b.append(definition.getInternalName().replace('/', '.'));
        if (definition.isHidden()) {
            b.append('/').append(ENCODER.encodeToString(definition.getDigest())).append('.').append(definition.getHiddenClassIndex());
        }
        return b;
    }
}
