package org.qbicc.graph;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.UnresolvedType;
import org.qbicc.type.definition.element.ExecutableElement;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

/**
 * A value handle for the target of an object reference value.
 */
public final class ReferenceHandle extends AbstractValueHandle {
    private final Value referenceValue;
    private final PointerType pointerType;

    ReferenceHandle(Node callSite, ExecutableElement element, int line, int bci, Value referenceValue) {
        super(callSite, element, line, bci);
        this.referenceValue = referenceValue;
        if (referenceValue.getType() instanceof ReferenceType rt) {
            pointerType = rt.getUpperBound().getPointer();
        } else if (referenceValue.getType() instanceof UnresolvedType) {
            pointerType = referenceValue.getType().getPointer();
        } else {
            throw new IllegalArgumentException("Invalid type for referenceValue: "+referenceValue.getType());
        }
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    public PointerType getType() {
        return pointerType;
    }

    @Override
    public ObjectType getPointeeType() {
        return (ObjectType) super.getPointeeType();
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? referenceValue : Util.throwIndexOutOfBounds(index);
    }

    public Value getReferenceValue() {
        return referenceValue;
    }

    int calcHashCode() {
        return referenceValue.hashCode();
    }

    @Override
    String getNodeName() {
        return "Reference";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReferenceHandle && equals((ReferenceHandle) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        referenceValue.toString(b);
        b.append(')');
        return b;
    }

    public boolean equals(ReferenceHandle other) {
        return this == other || other != null && referenceValue.equals(other.referenceValue);
    }

    /**
     * Reference handles are not writable.
     *
     * @return {@code false} always
     */
    @Override
    public boolean isWritable() {
        return false;
    }

    public boolean isConstantLocation() {
        return false;
    }

    @Override
    public boolean isValueConstant() {
        return referenceValue.isConstant();
    }

    @Override
    public AccessMode getDetectedMode() {
        return SinglePlain;
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueHandleVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
