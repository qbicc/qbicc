package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.element.Element;

/**
 *
 */
public final class Convert extends AbstractWordCastValue {
    Convert(final Element element, final int line, final int bci, final Value value, final WordType toType) {
        super(element, line, bci, value, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
