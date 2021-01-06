package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.PointerType;

/**
 *
 */
public class MemberPointer extends AbstractValue {
    private final Value structPointer;
    private final CompoundType.Member member;

    MemberPointer(final int line, final int bci, final Value structPointer, final CompoundType.Member member) {
        super(line, bci);
        this.structPointer = structPointer;
        this.member = member;
    }

    public Value getStructPointer() {
        return structPointer;
    }

    public CompoundType.Member getMember() {
        return member;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? structPointer : Util.throwIndexOutOfBounds(index);
    }

    int calcHashCode() {
        return Objects.hash(structPointer, member);
    }

    public boolean equals(final Object other) {
        return other instanceof MemberPointer && equals((MemberPointer) other);
    }

    public boolean equals(final MemberPointer other) {
        return this == other || other != null && structPointer.equals(other.structPointer) && member.equals(other.member);
    }

    public PointerType getType() {
        boolean const_ = structPointer.getType().isConst();
        PointerType pointerType = member.getType().getPointer();
        return const_ ? pointerType.asConst() : pointerType;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
