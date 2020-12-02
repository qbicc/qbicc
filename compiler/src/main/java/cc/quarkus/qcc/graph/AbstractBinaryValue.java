package cc.quarkus.qcc.graph;

import java.util.Objects;

/**
 *
 */
abstract class AbstractBinaryValue extends AbstractValue implements BinaryValue {
    final Value left;
    final Value right;

    AbstractBinaryValue(final int line, final int bci, final Value left, final Value right) {
        super(line, bci);
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
