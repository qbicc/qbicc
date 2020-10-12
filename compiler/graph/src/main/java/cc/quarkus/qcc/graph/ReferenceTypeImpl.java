package cc.quarkus.qcc.graph;

import io.smallrye.common.constraint.Assert;

final class ReferenceTypeImpl extends AbstractType implements ReferenceType {
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

    public ArrayClassType getArrayClassType() {
        return null;
    }
}
