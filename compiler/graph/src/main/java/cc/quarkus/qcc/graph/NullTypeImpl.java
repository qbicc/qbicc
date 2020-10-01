package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import io.smallrye.common.constraint.Assert;

final class NullTypeImpl extends NodeImpl implements NullType {
    public int getParameterCount() {
        return 0;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public Object boxValue(final ConstantValue value) {
        // todo: this probably isn't going to work long-term due to Maps being picky about null values...
        return null;
    }

    public void replaceWith(final Node node) {
        throw Assert.unsupported();
    }

    public String getLabelForGraph() {
        return "null";
    }
}
