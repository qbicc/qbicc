package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
public final class Div extends AbstractBinaryValue implements NonCommutativeBinaryValue, OrderedNode {
    private final Node dependency;

    Div(final ProgramLocatable pl, final Value v1, final Value v2, Node dependency) {
        super(pl, v1, v2);
        // TODO: implementing OrderedNode is temporary
        while (dependency instanceof OrderedNode on) {
            dependency = on.getDependency();
        }
        this.dependency = dependency;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    int calcHashCode() {
        return super.calcHashCode() * 19 + dependency.hashCode();
    }

    @Override
    boolean equals(AbstractBinaryValue other) {
        return super.equals(other) && other instanceof Div div && dependency.equals(div.dependency);
    }

    @Override
    String getNodeName() {
        return "Div";
    }
}
