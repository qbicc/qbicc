package cc.quarkus.qcc.type;

import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;

/**
 *
 */
public final class InterfaceObjectType extends ObjectType {
    private final DefinedTypeDefinition definition;
    private final List<InterfaceObjectType> interfaces;

    InterfaceObjectType(final TypeSystem typeSystem, final boolean const_, final DefinedTypeDefinition definition, final List<InterfaceObjectType> interfaces) {
        super(typeSystem, Objects.hash(definition), const_);
        this.definition = definition;
        this.interfaces = interfaces;
    }

    public long getSize() {
        return 0;
    }

    public int getAlign() {
        return 0;
    }

    public InterfaceObjectType asConst() {
        return (InterfaceObjectType) super.asConst();
    }

    public DefinedTypeDefinition getDefinition() {
        return definition;
    }

    public boolean hasSuperClass() {
        return false;
    }

    InterfaceObjectType constructConst() {
        return new InterfaceObjectType(typeSystem, true, definition, interfaces);
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

    public StringBuilder toString(final StringBuilder b) {
        return super.toString(b).append("interface").append('(').append(definition.getInternalName()).append(')');
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("interface").append('.').append(definition.getInternalName().replace('/', '-'));
    }
}
