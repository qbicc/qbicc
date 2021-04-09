package org.qbicc.type;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.qbicc.type.definition.DefinedTypeDefinition;

/**
 *
 */
public final class InterfaceObjectType extends ObjectType {
    private final DefinedTypeDefinition definition;
    private final List<InterfaceObjectType> interfaces;

    InterfaceObjectType(final TypeSystem typeSystem, final DefinedTypeDefinition definition, final List<InterfaceObjectType> interfaces) {
        super(typeSystem, Objects.hash(definition));
        this.definition = definition;
        this.interfaces = interfaces;
    }

    public long getSize() {
        return 0;
    }

    public int getAlign() {
        return 0;
    }

    public DefinedTypeDefinition getDefinition() {
        return definition;
    }

    ReferenceType createReferenceType() {
        return typeSystem.createReference(getRootType(), Set.of(this));
    }

    public boolean hasSuperClass() {
        return false;
    }

    public boolean isSubtypeOf(final ObjectType other) {
        return this == other
            || other instanceof InterfaceObjectType && isSubtypeOf((InterfaceObjectType) other)
            || other instanceof ClassObjectType && isSubtypeOf((ClassObjectType) other);
    }

    public boolean isSubtypeOf(final InterfaceObjectType other) {
        if (this == other) {
            return true;
        }
        for (InterfaceObjectType interface_ : interfaces) {
            if (interface_.isSubtypeOf(other)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSubtypeOf(final ClassObjectType other) {
        return other.getSuperClassType() == null; // j.l.O
    }

    public ObjectType getCommonSupertype(final ObjectType other) {
        if (other instanceof ClassObjectType) {
            return other.getCommonSupertype(this);
        } else if (other instanceof InterfaceObjectType) {
            if (isSubtypeOf(other)) {
                return other;
            } else if (isSupertypeOf(other)) {
                return this;
            } else {
                return getRootType();
            }
        } else {
            assert other instanceof ArrayObjectType;
            return getRootType();
        }
    }

    private PhysicalObjectType getRootType() {
        // todo: this could be done more elegantly
        return (PhysicalObjectType) definition.getContext().findDefinedType("java/lang/Object").load().getType();
    }

    public StringBuilder toString(final StringBuilder b) {
        return b.append("interface").append('(').append(definition.getInternalName()).append(')');
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("interface").append('.').append(definition.getInternalName().replace('/', '-'));
    }

    @Override
    public final boolean equals(ObjectType other) {
        return other instanceof InterfaceObjectType && equals((InterfaceObjectType) other);
    }

    public boolean equals(InterfaceObjectType other) {
        return super.equals(other) && definition == other.definition;
    }
}
