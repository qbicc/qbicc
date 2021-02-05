package cc.quarkus.qcc.type;

import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;

/**
 * A physical object that is some kind of array.
 */
public abstract class ArrayObjectType extends PhysicalObjectType {

    private final ClassObjectType objectClass;

    ArrayObjectType(final TypeSystem typeSystem, final int hashCode, final boolean const_, final ClassObjectType objectClass) {
        super(typeSystem, hashCode, const_);
        this.objectClass = objectClass;
    }

    public boolean hasSuperClass() {
        return true;
    }

    public ArrayObjectType asConst() {
        return (ArrayObjectType) super.asConst();
    }

    abstract ArrayObjectType constructConst();

    public ClassObjectType getSuperClassType() {
        return objectClass;
    }

    public abstract ValueType getElementType();

    public boolean isSubtypeOf(final ObjectType other) {
        return this == other
            || other instanceof ClassObjectType && isSubtypeOf((ClassObjectType) other)
            || other instanceof InterfaceObjectType && isSubtypeOf((InterfaceObjectType) other);
    }

    public boolean isSubtypeOf(final ClassObjectType other) {
        return other.getSuperClassType() == null; // j.l.O
    }

    public boolean isSubtypeOf(final InterfaceObjectType other) {
        // this is a little grisly but it means we don't have to retain references to these types
        DefinedTypeDefinition otherDef = other.getDefinition();
        if (otherDef.getContext() == otherDef.getContext().getCompilationContext().getBootstrapClassContext()) {
            return otherDef.internalPackageAndNameEquals("java/lang", "Cloneable") ||
                otherDef.internalPackageAndNameEquals("java/io", "Serializable");
        }
        return false;
    }

    public ObjectType getCommonSupertype(ObjectType other) {
        if (other instanceof InterfaceObjectType && isSubtypeOf((InterfaceObjectType) other)) {
            return other;
        } else {
            return getSuperClassType();
        }
    }

    public StringBuilder toString(final StringBuilder b) {
        return getElementType().toString(super.toString(b).append("array").append('[')).append(']');
    }
}
