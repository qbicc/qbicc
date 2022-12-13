package org.qbicc.graph.literal;

import java.util.Objects;

import org.qbicc.graph.Value;
import org.qbicc.type.PointerType;

/**
 * A literal pointer that is offset from the base pointer by some amount.
 */
public final class OffsetFromLiteral extends Literal {
    final Literal basePointer;
    final Literal offset;

    OffsetFromLiteral(Literal basePointer, Literal offset) {
        this.basePointer = basePointer;
        this.offset = offset;
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> basePointer;
            case 1 -> this.offset;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public boolean equals(Literal other) {
        return other instanceof OffsetFromLiteral && equals((OffsetFromLiteral) other);
    }

    public boolean equals(OffsetFromLiteral other) {
        return other == this || other != null && basePointer.equals(other.basePointer) && offset.equals(other.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(OffsetFromLiteral.class, basePointer, offset);
    }

    @Override
    public PointerType getType() {
        return basePointer.getType(PointerType.class);
    }

    public Literal getBasePointer() {
        return basePointer;
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public Literal getOffset() {
        return offset;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return offset.toString(basePointer.toString(b).append('+'));
    }
}
