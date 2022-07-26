package org.qbicc.graph;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

import java.util.Objects;

/**
 * A base + offset to be used in an Unsafe memory access.
 */
public class UnsafeHandle extends AbstractValueHandle {
    private final ValueHandle base;
    private final Value offset;
    private final ValueType outputType;

    UnsafeHandle(Node callSite, ExecutableElement element, int line, int bci, ValueHandle base, Value offset, ValueType outputType) {
        super(callSite, element, line, bci);
        this.base = base;
        this.offset = offset;
        this.outputType = outputType;
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return base;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? this.offset : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public PointerType getType() {
        return outputType.getPointer();
    }

    @Override
    public boolean isConstantLocation() {
            return offset.isConstant() && base.isConstantLocation();
    }

    @Override
    public boolean isValueConstant() {
        return offset.isConstant() && base.isValueConstant();
    }

    @Override
    public AccessMode getDetectedMode() {
        return base.getDetectedMode();
    }

    public ValueHandle getBase() {
        return base;
    }

    public Value getOffset() {
        return offset;
    }

    public ValueType getOutputType() {
        return outputType;
    }

    int calcHashCode() {
        return Objects.hash(base, offset, outputType);
    }

    @Override
    String getNodeName() {
        return "Unsafe";
    }

    public boolean equals(final Object other) {
        return other instanceof UnsafeHandle && equals((UnsafeHandle) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        offset.toString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final UnsafeHandle other) {
        return this == other || other != null && base.equals(other.base) && offset.equals(other.offset) && outputType.equals(other.offset);
    }

    public <T, R> R accept(final ValueHandleVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
