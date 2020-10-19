package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.TypeIdType;

/**
 *
 */
public final class InterfaceTypeIdLiteral extends TypeIdLiteral {

    public static final InterfaceTypeIdLiteral[] NONE = new InterfaceTypeIdLiteral[0];

    InterfaceTypeIdLiteral(final String interfaceName, final InterfaceTypeIdLiteral[] interfaces, final TypeIdType type) {
        super(interfaceName, null, interfaces, type);
    }

    public boolean isInterface() {
        return true;
    }

    public boolean isSubtypeOf(final TypeIdLiteral other) {
        return other instanceof InterfaceTypeIdLiteral && isSubtypeOf((InterfaceTypeIdLiteral) other);
    }

    public boolean isSubtypeOf(final InterfaceTypeIdLiteral other) {
        if (equals(other)) {
            return true;
        }
        for (TypeIdLiteral value : interfaces) {
            if (value.isSubtypeOf(other)) {
                return true;
            }
        }
        return false;
    }

    public String getInternalName() {
        return typeName;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
