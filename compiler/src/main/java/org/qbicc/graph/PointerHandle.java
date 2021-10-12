package org.qbicc.graph;

import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class PointerHandle extends AbstractValueHandle {
    private final Value pointerValue;
    private final PointerType pointerType;

    PointerHandle(Node callSite, ExecutableElement element, int line, int bci, Value pointerValue) {
        super(callSite, element, line, bci);
        this.pointerValue = pointerValue;
        pointerType = (PointerType) pointerValue.getType();
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? pointerValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public boolean isWritable() {
        ValueType pointeeType = pointerType.getPointeeType();
        return pointeeType.isComplete() && ! pointerType.isConstPointee();
    }

    @Override
    public boolean isConstantLocation() {
        return pointerValue.isConstant();
    }

    @Override
    public boolean isValueConstant() {
        return pointerValue.isConstant();
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return MemoryAtomicityMode.UNORDERED;
    }

    public PointerType getPointerType() {
        return pointerType;
    }

    int calcHashCode() {
        return pointerValue.hashCode();
    }

    @Override
    String getNodeName() {
        return "Pointer";
    }

    public Value getPointerValue() {
        return pointerValue;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PointerHandle && equals((PointerHandle) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        pointerValue.toString(b);
        b.append(')');
        return b;
    }

    public boolean equals(PointerHandle other) {
        return this == other || other != null && pointerValue.equals(other.pointerValue);
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
