package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
abstract class AbstractBinaryValue extends AbstractValue implements BinaryValue {
    final Value left;
    final Value right;

    AbstractBinaryValue(ProgramLocatable pl, Value left, Value right) {
        super(pl);
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
        if (this instanceof CommutativeBinaryValue) {
            return getClass().hashCode() * 19 + (left.hashCode() ^ right.hashCode());
        } else {
            assert this instanceof NonCommutativeBinaryValue;
            return Objects.hash(getClass(), left, right);
        }
    }

    public boolean equals(final Object other) {
        return other.getClass().equals(getClass()) && equals((AbstractBinaryValue) other);
    }

    boolean equals(final AbstractBinaryValue other) {
        return this == other || getClass() == other.getClass()
            && (left.equals(other.left) && right.equals(other.right)
                || this instanceof CommutativeBinaryValue && left.equals(other.right) && right.equals(other.left));
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        getLeftInput().toReferenceString(b);
        b.append(',');
        getRightInput().toReferenceString(b);
        b.append(')');
        return b;
    }
}
