package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;

/**
 * A pointer that is offset from another pointer by some number of bytes.
 */
public final class ByteOffsetPointer extends AbstractValue {
    private final Value base;
    private final Value offset;
    private final ValueType outputType;

    ByteOffsetPointer(final ProgramLocatable pl, Value base, Value offset, ValueType outputType) {
        super(pl);
        this.base = base;
        this.offset = offset;
        this.outputType = outputType;
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> base;
            case 1 -> offset;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public PointerType getType() {
        return outputType.getPointer();
    }

    @Override
    public boolean isConstant() {
        return offset.isConstant() && base.isConstant();
    }

    @Override
    public boolean isPointeeConstant() {
        return false;
    }

    @Override
    public AccessMode getDetectedMode() {
        return base.getDetectedMode();
    }

    public Value getBasePointer() {
        return base;
    }

    public Value getOffset() {
        return offset;
    }

    public ValueType getOutputType() {
        return getPointeeType();
    }

    int calcHashCode() {
        return Objects.hash(base, offset, outputType);
    }

    @Override
    String getNodeName() {
        return "ByteOffsetPointer";
    }

    public boolean equals(final Object other) {
        return other instanceof ByteOffsetPointer && equals((ByteOffsetPointer) other);
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        return offset.toReferenceString(base.toReferenceString(b.append("offset ")).append(" by bytes "));
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        offset.toReferenceString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final ByteOffsetPointer other) {
        return this == other || other != null && base.equals(other.base) && offset.equals(other.offset) && outputType.equals(other.outputType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
