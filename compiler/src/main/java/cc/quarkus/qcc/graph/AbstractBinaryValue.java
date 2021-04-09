package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
abstract class AbstractBinaryValue extends AbstractValue implements BinaryValue {
    final Value left;
    final Value right;

    AbstractBinaryValue(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value left, final Value right) {
        super(callSite, element, line, bci);
        this.left = left;
        this.right = right;
    }

    public Value getLeftInput() {
        return left;
    }

    public Value getRightInput() {
        return right;
    }

    int calcHashCode() {
        return Objects.hash(getClass(), left, right);
    }

    public boolean equals(final Object other) {
        return other.getClass().equals(getClass()) && equals((AbstractBinaryValue) other);
    }

    boolean equals(final AbstractBinaryValue other) {
        return this == other || getClass() == other.getClass() && left.equals(other.left) && right.equals(other.right);
    }
}
