package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.object.Function;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class PointerHandle extends AbstractValueHandle {
    private final Value pointerValue;
    private final Value offsetValue;
    private final PointerType pointerType;

    PointerHandle(Node callSite, ExecutableElement element, int line, int bci, Value pointerValue, Value offsetValue) {
        super(callSite, element, line, bci);
        this.pointerValue = pointerValue;
        pointerType = (PointerType) pointerValue.getType();
        this.offsetValue = offsetValue;
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? pointerValue : index == 1 ? offsetValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public boolean isWritable() {
        ValueType pointeeType = pointerType.getPointeeType();
        return pointeeType.isComplete() && ! pointerType.isConstPointee();
    }

    @Override
    public boolean isNoReturn() {
        if (pointerValue instanceof ProgramObjectLiteral pol) {
            if (pol.getProgramObject() instanceof Function fn) {
                return fn.isNoReturn();
            }
        }
        return super.isNoReturn();
    }

    @Override
    public boolean isNoSideEffect() {
        if (pointerValue instanceof ProgramObjectLiteral pol) {
            if (pol.getProgramObject() instanceof Function fn) {
                return fn.isNoSideEffects();
            }
        }
        return super.isNoSideEffect();
    }

    @Override
    public boolean isConstantLocation() {
        return pointerValue.isConstant() && offsetValue.isConstant();
    }

    @Override
    public boolean isValueConstant() {
        // todo: read-only section pointers
        return false;
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return MemoryAtomicityMode.UNORDERED;
    }

    public PointerType getPointerType() {
        return pointerType;
    }

    int calcHashCode() {
        return Objects.hash(pointerValue, offsetValue);
    }

    @Override
    String getNodeName() {
        return "Pointer";
    }

    public Value getPointerValue() {
        return pointerValue;
    }

    public Value getOffsetValue() {
        return offsetValue;
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
        b.append('[');
        offsetValue.toString(b);
        b.append(']');
        b.append(')');
        return b;
    }

    public boolean equals(PointerHandle other) {
        return this == other || other != null && pointerValue.equals(other.pointerValue) && offsetValue.equals(other.offsetValue);
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
