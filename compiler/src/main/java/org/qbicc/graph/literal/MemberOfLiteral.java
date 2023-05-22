package org.qbicc.graph.literal;

import java.util.Objects;

import org.qbicc.graph.Value;
import org.qbicc.type.StructType;
import org.qbicc.type.PointerType;
import org.qbicc.type.WordType;

/**
 * A literal pointer that is a pointer to a member of the given structure.
 */
public final class MemberOfLiteral extends Literal {
    final Literal structurePointer;
    final StructType.Member member;

    MemberOfLiteral(Literal structurePointer, StructType.Member member) {
        this.structurePointer = structurePointer;
        this.member = member;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> structurePointer;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public boolean equals(Literal other) {
        return other instanceof MemberOfLiteral && equals((MemberOfLiteral) other);
    }

    public boolean equals(MemberOfLiteral other) {
        return other == this || other != null && structurePointer.equals(other.structurePointer) && member.equals(other.member);
    }

    @Override
    public int hashCode() {
        return Objects.hash(MemberOfLiteral.class, structurePointer, member);
    }

    @Override
    Literal bitCast(LiteralFactory lf, WordType toType) {
        if (toType.equals(structurePointer.getType()) && member.getOffset() == 0) {
            // bit cast from first element of structure to base
            return structurePointer;
        }
        return super.bitCast(lf, toType);
    }

    @Override
    public PointerType getType() {
        return member.getType().getPointer();
    }

    public Literal getStructurePointer() {
        return structurePointer;
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public StructType.Member getMember() {
        return member;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return member.toString(structurePointer.toString(b).append('+'));
    }
}
