package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import io.smallrye.common.constraint.Assert;

final class VoidTypeImpl extends NodeImpl implements VoidType {
    public ArrayClassType getArrayClassType() {
        throw new UnsupportedOperationException();
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

    public void replaceWith(final Node node) {
        throw Assert.unsupported();
    }

    public String getLabelForGraph() {
        return "void";
    }
}
