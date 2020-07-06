package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
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
        return definition.getName();
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

    public int getParameterCount() {
        return 0;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public String getLabelForGraph() {
        return "interface[" + getClassName() + "]";
    }

    @Override
    public String toString() {
        return "InterfaceTypeImpl{" +
                "className='" + getClassName() + '\'' +
                '}';
    }
}
