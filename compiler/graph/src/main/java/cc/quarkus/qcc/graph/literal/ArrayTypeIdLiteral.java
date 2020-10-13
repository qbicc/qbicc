package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.type.TypeIdType;
import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public abstract class ArrayTypeIdLiteral extends RealTypeIdLiteral {
    ArrayTypeIdLiteral(final ClassTypeIdLiteral superClass, final InterfaceTypeIdLiteral[] interfaces, final TypeIdType type) {
        super(superClass, interfaces, type);
    }

    public abstract ValueType getElementType();

    public boolean isArray() {
        return true;
    }
}
