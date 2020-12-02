package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.TypeIdType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A constant value whose type is a {@link TypeIdType}.
 */
public abstract class TypeIdLiteral extends Literal {

    final String typeName;
    final ClassTypeIdLiteral superClass;
    final InterfaceTypeIdLiteral[] interfaces;
    final TypeIdType type;

    TypeIdLiteral(final String typeName, final ClassTypeIdLiteral superClass, final InterfaceTypeIdLiteral[] interfaces, final TypeIdType type) {
        this.typeName = typeName;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.type = type;
    }

    public abstract boolean isInterface();

    public boolean isClass() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public abstract boolean isSubtypeOf(TypeIdLiteral other);

    public final boolean isSupertypeOf(TypeIdLiteral other) {
        return other.isSubtypeOf(this);
    }

    public Literal withTypeRaw(final Type type) {
        if (type != this.type) {
            throw new IllegalArgumentException("Cannot cast type ID values");
        }
        return this;
    }

    public ValueType getType() {
        return type;
    }

    public boolean hasSuperClass() {
        return superClass != null;
    }

    public ClassTypeIdLiteral getSuperClass() {
        return superClass;
    }

    public int getInterfaceCount() {
        return interfaces.length;
    }

    public TypeIdLiteral getInterface(int index) {
        return interfaces[index];
    }

    public boolean equals(final Literal other) {
        // every instance is unique
        return this == other;
    }

    public int hashCode() {
        // every instance is unique
        return System.identityHashCode(this);
    }

    public String toString() {
        return typeName;
    }
}
