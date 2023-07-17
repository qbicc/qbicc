package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.WordType;

/**
 * Represents the bitwise complement of the integer or boolean input.
 */
public final class Comp extends AbstractUnaryValue {
    Comp(final ProgramLocatable pl, final Value v) {
        super(pl, v);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public WordType getType() {
        return (WordType) super.getType();
    }

    @Override
    public boolean isDefNe(Value other) {
        return other.isDefEq(input) || super.isDefNe(other);
    }

    @Override
    public Value getValueIfTrue(BasicBlockBuilder bbb, Value input) {
        return getInput().getValueIfFalse(bbb, input);
    }

    @Override
    public Value getValueIfFalse(BasicBlockBuilder bbb, Value input) {
        return getInput().getValueIfTrue(bbb, input);
    }

    @Override
    String getNodeName() {
        return "Comp";
    }
}
