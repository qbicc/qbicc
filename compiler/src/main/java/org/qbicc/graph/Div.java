package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class Div extends AbstractBinaryValue implements NonCommutativeBinaryValue, OrderedNode {
    private final Node dependency;

    Div(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v1, final Value v2, Node dependency) {
        super(callSite, element, line, bci, v1, v2);
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
