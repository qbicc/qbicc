package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import io.smallrye.common.constraint.Assert;

final class ReferenceTypeImpl extends NodeImpl implements ReferenceType {
    private final ClassType upperBound;
    private final ClassType lowerBound;

    ReferenceTypeImpl(final ClassType upperBound, final ClassType lowerBound) {
        this.upperBound = Assert.checkNotNullParam("upperBound", upperBound);
        this.lowerBound = lowerBound;
    }

    public ClassType getUpperBound() {
        return null;
    }

    public ClassType getLowerBound() {
        return null;
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
        StringBuilder b = new StringBuilder();
        b.append("reference").append('[');
        b.append('+').append(upperBound);
        ClassType lowerBound = this.lowerBound;
        if (lowerBound != null) {
            b.append(',').append('-').append(lowerBound);
        }
        b.append(']');
        return b.toString();
    }
}
