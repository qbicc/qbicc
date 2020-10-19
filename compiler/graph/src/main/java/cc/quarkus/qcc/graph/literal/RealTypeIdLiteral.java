package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.type.TypeIdType;

/**
 * A literal representing a real (non-interface) type.
 */
public abstract class RealTypeIdLiteral extends TypeIdLiteral {

    RealTypeIdLiteral(final String typeName, final ClassTypeIdLiteral superClass, final InterfaceTypeIdLiteral[] interfaces, final TypeIdType type) {
        super(typeName, superClass, interfaces, type);
    }

    public boolean isInterface() {
        return false;
    }
}
