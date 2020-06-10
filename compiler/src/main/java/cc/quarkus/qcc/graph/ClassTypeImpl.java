package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class ClassTypeImpl extends NodeImpl implements ClassType {
    private final String className;

    ClassTypeImpl(final String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
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
