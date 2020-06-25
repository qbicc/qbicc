package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class ClassTypeImpl extends AbstractClassTypeImpl implements ClassType {
    private final String className;
    private final ClassType superClass;
    private final InterfaceType[] interfaces;

    ClassTypeImpl(final String className, final ClassType superClass, final InterfaceType[] interfaces) {
        this.className = className;
        this.superClass = superClass;
        this.interfaces = interfaces;
    }

    public String getClassName() {
        return className;
    }

    public ClassType getSuperClass() {
        return superClass;
    }

    public int getInterfaceCount() {
        return interfaces.length;
    }

    public InterfaceType getInterface(final int index) throws IndexOutOfBoundsException {
        return interfaces[index];
    }

    public boolean isSuperTypeOf(final ClassType other) {
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
        return "class[" + className + "]";
    }

    @Override
    public String toString() {
        return "ClassTypeImpl{" +
                "className='" + className + '\'' +
                '}';
    }
}
