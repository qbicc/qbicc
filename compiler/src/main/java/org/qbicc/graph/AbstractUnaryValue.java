package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

abstract class AbstractUnaryValue extends AbstractValue implements UnaryValue {
    final Value input;

    AbstractUnaryValue(final ProgramLocatable pl, final Value input) {
        super(pl);
        this.input = input;
    }

    public Value getInput() {
        return input;
    }

    int calcHashCode() {
        return Objects.hash(getClass(), input);
    }

    public boolean equals(final Object other) {
        return other.getClass() == getClass() && equals((AbstractUnaryValue) other);
    }

    boolean equals(final AbstractUnaryValue other) {
        return this == other || other != null && input.equals(other.input);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        getInput().toReferenceString(b);
        b.append(')');
        return b;
    }
}
