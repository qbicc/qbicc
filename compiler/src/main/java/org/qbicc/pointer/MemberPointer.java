package org.qbicc.pointer;

import org.qbicc.interpreter.Memory;
import org.qbicc.type.StructType;

/**
 *
 */
public final class MemberPointer extends Pointer {
    private final Pointer structurePointer;
    private final StructType.Member member;

    public MemberPointer(Pointer structurePointer, StructType.Member member) {
        super(member.getType().getPointer());
        this.structurePointer = structurePointer;
        this.member = member;
    }

    @Override
    public StructType getPointeeType() {
        return (StructType) super.getPointeeType();
    }

    public Pointer getStructurePointer() {
        return structurePointer;
    }

    public StructType.Member getMember() {
        return member;
    }

    @Override
    public RootPointer getRootPointer() {
        return structurePointer.getRootPointer();
    }

    @Override
    public long getRootByteOffset() {
        return structurePointer.getRootByteOffset() + member.getOffset();
    }

    @Override
    public Memory getRootMemoryIfExists() {
        return structurePointer.getRootMemoryIfExists();
    }

    @Override
    public String getRootSymbolIfExists() {
        return structurePointer.getRootSymbolIfExists();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return structurePointer.toString(b).append('.').append(member.getName());
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
