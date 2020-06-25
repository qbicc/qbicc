package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class InterfaceTypeImpl extends AbstractClassTypeImpl implements InterfaceType {
    private final String className;
    private final InterfaceType[] interfaces;

    InterfaceTypeImpl(final String className, final InterfaceType[] interfaces) {
        this.className = className;
        this.interfaces = interfaces;
    }

    public String getClassName() {
        return className;
    }

    public ClassType getSuperClass() {
        return null;
    }

    public int getInterfaceCount() {
        return interfaces.length;
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
        return "interface[" + className + "]";
    }

    @Override
    public String toString() {
        return "InterfaceTypeImpl{" +
                "className='" + className + '\'' +
                '}';
    }
}
