package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;

/**
 *
 */
final class InterfaceTypeImpl extends AbstractClassTypeImpl implements InterfaceType {
    private final VerifiedTypeDefinition definition;
    private final InterfaceType[] interfaces;

    InterfaceTypeImpl(VerifiedTypeDefinition definition, final InterfaceType[] interfaces) {
        this.definition = definition;
        this.interfaces = interfaces;
    }

    public String getClassName() {
        return definition.getInternalName();
    }

    public ClassType getSuperClass() {
        return null;
    }

    public int getInterfaceCount() {
        return interfaces.length;
    }

    public VerifiedTypeDefinition getDefinition() {
        return definition;
    }

    public InterfaceType getInterface(final int index) throws IndexOutOfBoundsException {
        return interfaces[index];
    }

    @Override
    public String toString() {
        return "InterfaceTypeImpl{" +
                "className='" + getClassName() + '\'' +
                '}';
    }
}
