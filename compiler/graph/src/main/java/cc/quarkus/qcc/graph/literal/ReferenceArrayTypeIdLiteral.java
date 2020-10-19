package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeIdType;

/**
 *
 */
public final class ReferenceArrayTypeIdLiteral extends ArrayTypeIdLiteral {

    private final ReferenceType elementType;

    ReferenceArrayTypeIdLiteral(final ClassTypeIdLiteral objectClass, final TypeIdType type, final ReferenceType elementType, final TypeIdLiteral elementUpperBound) {
        super("[L" + elementUpperBound + ";", objectClass, InterfaceTypeIdLiteral.NONE, type);
        this.elementType = elementType;
    }

    public ReferenceType getElementType() {
        return elementType;
    }

    public TypeIdLiteral getElementUpperBound() {
        return elementType.getUpperBound();
    }

    public boolean isSubtypeOf(final TypeIdLiteral other) {
        // true if other is object (our superclass), or if our element type upper bound is a subtype of theirs
        return other == this
            || other == getSuperClass()
            || other instanceof ReferenceArrayTypeIdLiteral
                && getElementUpperBound().isSubtypeOf(((ReferenceArrayTypeIdLiteral) other).getElementUpperBound());
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
