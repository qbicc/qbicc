package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.CompoundType;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A handle for a structure member.  The input handle must have compound type.
 */
public final class MemberOf extends AbstractValueHandle {
    private final ValueHandle structureHandle;
    private final PointerType pointerType;
    private final CompoundType structType;
    private final CompoundType.Member member;

    MemberOf(Node callSite, ExecutableElement element, int line, int bci, ValueHandle structureHandle, CompoundType.Member member) {
        super(callSite, element, line, bci);
        this.structureHandle = structureHandle;
        this.member = member;

        pointerType = member.getType().getPointer().withQualifiersFrom(structureHandle.getPointerType());
        structType = (CompoundType) structureHandle.getValueType();
        if (! structType.getMembers().contains(member)) {
            throw new IllegalStateException(String.format("Compound %s does not contain %s", structType, member));
        }
    }

    public CompoundType getStructType() {
        return structType;
    }

    @Override
    public PointerType getPointerType() {
        return pointerType;
    }

    @Override
    public boolean isConstantLocation() {
        return structureHandle.isConstantLocation();
    }

    @Override
    public boolean isValueConstant() {
        return structureHandle.isValueConstant();
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return structureHandle.getDetectedMode();
    }

    public CompoundType.Member getMember() {
        return member;
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return structureHandle;
    }

    int calcHashCode() {
        return Objects.hash(structureHandle, member);
    }

    public boolean equals(final Object other) {
        return other instanceof MemberOf && equals((MemberOf) other);
    }

    public boolean equals(final MemberOf other) {
        return this == other || other != null && structureHandle.equals(other.structureHandle) && member.equals(other.member);
    }

    public <T, R> R accept(final ValueHandleVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
