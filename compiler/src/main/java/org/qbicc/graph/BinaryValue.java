package org.qbicc.graph;

import org.qbicc.type.ValueType;

/**
 *
 */
public interface BinaryValue extends Value {
    Value getLeftInput();
    Value getRightInput();

    default ValueType getType() {
        return getLeftInput().getType();
    }

    default boolean isConstant() {
        return getLeftInput().isConstant() && getRightInput().isConstant();
    }

    default int getValueDependencyCount() {
        return 2;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getLeftInput() : index == 1 ? getRightInput() : Util.throwIndexOutOfBounds(index);
    }
}
