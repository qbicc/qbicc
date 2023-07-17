package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;

/**
 * A value that asserts non-nullity.
 */
public final class NotNull extends AbstractUnaryValue {
    NotNull(ProgramLocatable pl, Value input) {
        super(pl, input);
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public Value unconstrained() {
        return getInput().unconstrained();
    }

    @Override
    String getNodeName() {
        return "NotNull";
    }
}
