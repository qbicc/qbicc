package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.Element;

/**
 *
 */
public final class Neg extends AbstractUnaryValue {
    Neg(final Element element, final int line, final int bci, final Value v) {
        super(element, line, bci, v);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
