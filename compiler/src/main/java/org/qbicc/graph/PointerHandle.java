package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.object.Function;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

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
    public boolean isNoReturn() {
        return pointerValue instanceof PointerLiteral pl
            && pl.getPointer() instanceof ProgramObjectPointer pop
            && pop.getProgramObject() instanceof Function fn
            && fn.isNoReturn()
            || super.isNoReturn();
    }

    @Override
    public boolean isNoSideEffect() {
        return pointerValue instanceof PointerLiteral pl
            && pl.getPointer() instanceof ProgramObjectPointer pop
            && pop.getProgramObject() instanceof Function fn
            && fn.isNoSideEffects()
            || super.isNoSideEffect();
    }

    @Override
    public boolean isNoSafePoints() {
        return pointerValue instanceof PointerLiteral pl
            && pl.getPointer() instanceof ProgramObjectPointer pop
            && pop.getProgramObject() instanceof Function fn
            && fn.isNoSafePoints()
            || super.isNoSafePoints();
    }

    @Override
    public boolean isConstantLocation() {
        return pointerValue.isConstant();
    }

    @Override
    public boolean isValueConstant() {
        // todo: read-only section pointers
        return false;
    }

    @Override
    public AccessMode getDetectedMode() {
        return SinglePlain;
    }

    public PointerType getType() {
        return pointerType;
    }

    int calcHashCode() {
        return Objects.hash(PointerHandle.class, pointerValue);
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
        pointerValue.toReferenceString(b);
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
