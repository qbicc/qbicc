package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An addition of two values.
 */
public final class Add extends AbstractBinaryValue implements CommutativeBinaryValue {
    Add(final ProgramLocatable pl, final Value v1, final Value v2) {
        super(pl, v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "Add";
    }
}
