package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.Element;

/**
 *
 */
public final class Div extends AbstractBinaryValue implements NonCommutativeBinaryValue {
    Div(final Element element, final int line, final int bci, final Value v1, final Value v2) {
        super(element, line, bci, v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
