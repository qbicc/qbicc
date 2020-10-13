package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public interface BinaryValue extends Value {
    Value getLeftInput();
    Value getRightInput();

    default ValueType getType() {
        return getLeftInput().getType();
    }

    default int getValueDependencyCount() {
        return 2;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getLeftInput() : index == 1 ? getRightInput() : Util.throwIndexOutOfBounds(index);
    }
}
