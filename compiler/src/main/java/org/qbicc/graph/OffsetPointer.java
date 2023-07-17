package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.PointerType;

/**
 * A pointer that is offset from the base pointer by a number of base-pointee-typed elements.
 */
public final class OffsetPointer extends AbstractValue {
    private final Value basePointer;
    private final Value offset;

    OffsetPointer(final ProgramLocatable pl, Value basePointer, Value offset) {
        super(pl);
        this.basePointer = basePointer;
        this.offset = offset;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(OffsetPointer.class, basePointer, offset);
    }

    @Override
    String getNodeName() {
        return "OffsetPointer";
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> basePointer;
            case 1 -> offset;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OffsetPointer op && equals(op);
    }

    public boolean equals(OffsetPointer other) {
        return this == other || other != null && basePointer.equals(other.basePointer) && offset.equals(other.offset);
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        return offset.toReferenceString(basePointer.toReferenceString(b.append("offset ")).append(" by "));
    }

    @Override
    public PointerType getType() {
        return basePointer.getType(PointerType.class);
    }

    public Value getBasePointer() {
        return basePointer;
    }

    public Value getOffset() {
        return offset;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
