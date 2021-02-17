package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class PointerHandle extends AbstractValueHandle {
    private final Value pointerValue;
    private final PointerType pointerType;
    private final ValueType valueType;

    PointerHandle(Node callSite, ExecutableElement element, int line, int bci, Value pointerValue) {
        super(callSite, element, line, bci);
        this.pointerValue = pointerValue;
        pointerType = (PointerType) pointerValue.getType();
        valueType = pointerType.getPointeeType();
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
    public MemoryAtomicityMode getDetectedMode() {
        return MemoryAtomicityMode.UNORDERED;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public PointerType getPointerType() {
        return pointerType;
    }

    int calcHashCode() {
        return pointerValue.hashCode();
    }

    public Value getPointerValue() {
        return pointerValue;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PointerHandle && equals((PointerHandle) other);
    }

    public boolean equals(PointerHandle other) {
        return this == other || other != null && pointerValue.equals(other.pointerValue);
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
