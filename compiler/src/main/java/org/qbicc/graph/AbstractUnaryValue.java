package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.definition.element.ExecutableElement;

abstract class AbstractUnaryValue extends AbstractValue implements UnaryValue {
    final Value input;

    AbstractUnaryValue(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value input) {
        super(callSite, element, line, bci);
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
}
