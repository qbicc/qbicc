package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.TypeIdType;

/**
 *
 */
public final class ClassTypeIdLiteral extends RealTypeIdLiteral {

    ClassTypeIdLiteral(final ClassTypeIdLiteral superClass, final InterfaceTypeIdLiteral[] interfaces, final TypeIdType type) {
        super(superClass, interfaces, type);
    }

    public boolean isSubtypeOf(final TypeIdLiteral other) {
        return other instanceof ClassTypeIdLiteral && isSubtypeOf((ClassTypeIdLiteral) other)
            || other instanceof InterfaceTypeIdLiteral && isSubtypeOf((InterfaceTypeIdLiteral) other);
    }

    public boolean isSubtypeOf(final ClassTypeIdLiteral other) {
        return equals(other) || hasSuperClass() && superClass.isSubtypeOf(other);
    }

    public boolean isSubtypeOf(final InterfaceTypeIdLiteral other) {
        for (InterfaceTypeIdLiteral value : interfaces) {
            if (value.isSubtypeOf(other)) {
                return true;
            }
        }
        return false;
    }

    public boolean isClass() {
        return true;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
