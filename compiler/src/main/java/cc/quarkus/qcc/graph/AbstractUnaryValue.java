package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.Element;

abstract class AbstractUnaryValue extends AbstractValue implements UnaryValue {
    final Value input;

    AbstractUnaryValue(final Element element, final int line, final int bci, final Value input) {
        super(element, line, bci);
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
