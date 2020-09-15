package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class UninitializedTypeImpl extends NodeImpl implements UninitializedType {
    private final AbstractClassTypeImpl classType;

    UninitializedTypeImpl(final AbstractClassTypeImpl classType) {
        this.classType = classType;
    }

    public ClassType getClassType() {
        return classType;
    }

    public boolean isAssignableFrom(final Type otherType) {
        return false;
    }

    public int getParameterCount() {
        return 0;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        return null;
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        return null;
    }

    public String getLabelForGraph() {
        return null;
    }
}
