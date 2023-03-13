package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.PointerType;
import org.qbicc.type.UnionType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A handle for a union member.  The input handle must have union type.
 */
public final class MemberOfUnion extends AbstractValue {
    private final Value unionPointer;
    private final PointerType pointerType;
    private final UnionType.Member member;

    MemberOfUnion(Node callSite, ExecutableElement element, int line, int bci, Value unionPointer, UnionType.Member member) {
        super(callSite, element, line, bci);
        this.unionPointer = unionPointer;
        this.member = member;
        pointerType = member.getType().getPointer().withQualifiersFrom(unionPointer.getType(PointerType.class));
    }

    public UnionType getUnionType() {
        return getUnionPointer().getType(PointerType.class).getPointeeType(UnionType.class);
    }

    @Override
    public PointerType getType() {
        return pointerType;
    }

    @Override
    public boolean isConstant() {
        return unionPointer.isConstant();
    }

    @Override
    public boolean isPointeeConstant() {
        return unionPointer.isPointeeConstant();
    }

    @Override
    public AccessMode getDetectedMode() {
        return unionPointer.getDetectedMode();
    }

    public UnionType.Member getMember() {
        return member;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> unionPointer;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    public Value getUnionPointer() {
        return unionPointer;
    }

    int calcHashCode() {
        return Objects.hash(unionPointer, member);
    }

    @Override
    String getNodeName() {
        return "MemberOfUnion";
    }

    public boolean equals(final Object other) {
        return other instanceof MemberOfUnion && equals((MemberOfUnion) other);
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        b.append("union member pointer ");
        unionPointer.toReferenceString(b);
        b.append('.');
        member.toString(b);
        return b;
    }

    public boolean equals(final MemberOfUnion other) {
        return this == other || other != null && unionPointer.equals(other.unionPointer) && member.equals(other.member);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
