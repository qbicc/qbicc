package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
public final class SafePoint extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;

    SafePoint(final ProgramLocatable pl, Node dependency) {
        super(pl);
        this.dependency = dependency;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    public boolean maySafePoint() {
        return true;
    }

    @Override
    String getNodeName() {
        return "SafePoint";
    }

    @Override
    int calcHashCode() {
        return Objects.hash(SafePoint.class, dependency);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SafePoint r && equals(r);
    }

    public boolean equals(SafePoint other) {
        return this == other || other != null && dependency.equals(other.dependency);
    }

    @Override
    public <T, R> R accept(ActionVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
