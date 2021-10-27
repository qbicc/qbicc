package org.qbicc.graph;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Wraps a CompoundType value which cannot be directly dereferenced. If this node is lowered an error will be thrown.
 */
public class Deref extends AbstractValue implements Value {
    private final Value value;

    Deref(Node callSite, ExecutableElement element, int line, int bci, Value value) {
        super(callSite, element, line, bci);
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    int calcHashCode() {
        return value.hashCode() * 19;
    }

    @Override
    String getNodeName() {
        return "Deref";
    }

    public boolean equals(final Object other) {
        return other instanceof Deref && equals((Deref) other);
    }

    public boolean equals(final Deref other) {
        return this == other || other != null && value.equals(other.value);
    }

    public ValueType getType() {
        return value.getType();
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}