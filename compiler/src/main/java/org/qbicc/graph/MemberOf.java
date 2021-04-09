package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.CompoundType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A handle for a structure member.  The input handle must have compound type.
 */
public final class MemberOf extends AbstractValueHandle {
    private final ValueHandle structureHandle;
    private final CompoundType structType;
    private final CompoundType.Member member;

    MemberOf(Node callSite, ExecutableElement element, int line, int bci, ValueHandle structureHandle, CompoundType.Member member) {
        super(callSite, element, line, bci);
        this.structureHandle = structureHandle;
        this.member = member;
        structType = (CompoundType) structureHandle.getValueType();
    }

    public CompoundType getStructType() {
        return structType;
    }

    @Override
    public ValueType getValueType() {
        return member.getType();
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
}
