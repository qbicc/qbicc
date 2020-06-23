package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import io.smallrye.common.constraint.Assert;

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

    public boolean isAssignableFrom(final ClassType other) {
        Assert.checkNotNullParam("other", other);

        if (this == Type.JAVA_LANG_OBJECT) {
            // all objects are assignable to JLO
            return true;
        }

        ClassType superClass = other.getSuperClass();

        return other == this || superClass != null && isAssignableFrom(superClass);
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
