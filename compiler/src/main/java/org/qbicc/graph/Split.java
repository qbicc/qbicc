package org.qbicc.graph;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.ValueType;

/**
 * A value split; used by the register allocator.
 */
public final class Split extends AbstractValue {
    private final Value input;

    Split(final ProgramLocatable pl, final Value input) {
        super(pl);
        this.input = Assert.checkNotNullParam("input", input);
    }

    @Override
    String getNodeName() {
        return "split";
    }

    public Value input() {
        return input;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> input;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Split other && equals(other);
    }

    public boolean equals(Split other) {
        return this == other || other != null && input.equals(other.input);
    }

    @Override
    int calcHashCode() {
        // multiply by a prime so that splits of splits accumulate a semi-unique hash code
        return input.hashCode() * 19;
    }

    @Override
    public ValueType getType() {
        return input.getType();
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
