package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeIdType;
import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public final class ValueArrayTypeIdLiteral extends ArrayTypeIdLiteral {

    private final ValueType elementType;

    ValueArrayTypeIdLiteral(final ClassTypeIdLiteral objectClass, final TypeIdType type, final ValueType elementType) {
        super(objectClass, InterfaceTypeIdLiteral.NONE, type);
        assert ! objectClass.hasSuperClass();
        assert ! (elementType instanceof ReferenceType);
        this.elementType = elementType;
    }

    public ValueType getElementType() {
        return elementType;
    }

    public boolean isSubtypeOf(final TypeIdLiteral other) {
        return other == this || other == getSuperClass();
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
