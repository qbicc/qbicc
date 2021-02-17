package cc.quarkus.qcc.type;

import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;

/**
 * A type which represents a class (<em>not</em> a reference to a class).
 */
public final class ClassObjectType extends PhysicalObjectType {
    private final DefinedTypeDefinition definition;
    private final ClassObjectType superClassType;
    private final List<InterfaceObjectType> interfaces;

    ClassObjectType(final TypeSystem typeSystem, final DefinedTypeDefinition definition, final ClassObjectType superClassType, final List<InterfaceObjectType> interfaces) {
        super(typeSystem, Objects.hash(definition));
        this.definition = definition;
        this.superClassType = superClassType;
        this.interfaces = interfaces;
    }

    public DefinedTypeDefinition getDefinition() {
        return definition;
    }

    public boolean hasSuperClass() {
        return superClassType != null;
    }

    public ClassObjectType getSuperClassType() {
        return superClassType;
    }

    public long getSize() {
        // todo: probe definition for layout size? or, maybe never report layout and rely on lowering to struct instead
        throw new IllegalStateException("Object layout has not yet taken place");
    }

    public boolean isSubtypeOf(final ObjectType other) {
        return this == other
            || other instanceof ClassObjectType && isSubtypeOf((ClassObjectType) other)
            || other instanceof InterfaceObjectType && isSubtypeOf((InterfaceObjectType) other);
    }

    public boolean isSubtypeOf(final ClassObjectType other) {
        return this == other
            || other.superClassType == null
            || superClassType != null
            && superClassType.isSubtypeOf(other);
    }

    public boolean isSubtypeOf(final InterfaceObjectType other) {
        for (InterfaceObjectType interface_ : interfaces) {
            if (interface_.isSubtypeOf(other)) {
                return true;
            }
        }
        return superClassType != null && superClassType.isSubtypeOf(other);
    }

    public ObjectType getCommonSupertype(final ObjectType other) {
        if (other instanceof ClassObjectType) {
            return getCommonSupertype((ClassObjectType) other);
        } else if (other instanceof InterfaceObjectType) {
            return isSubtypeOf(other) ? other : getRootType();
        } else {
            assert other instanceof ArrayObjectType;
            return getRootType();
        }
    }

    private ClassObjectType getRootType() {
        return superClassType != null ? superClassType.getRootType() : this;
    }

    public ClassObjectType getCommonSupertype(final ClassObjectType other) {
        return this.isSubtypeOf(other) ? other : this.isSupertypeOf(other) ? this : other.getSuperClassType().getCommonSupertype(this);
    }

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("class").append('(').append(definition.getInternalName()).append(')');
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("class").append('.').append(definition.getInternalName().replace('/', '-'));
    }
}
