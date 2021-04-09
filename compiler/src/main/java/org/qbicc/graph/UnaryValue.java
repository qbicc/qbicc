package org.qbicc.graph;

import org.qbicc.type.ValueType;

/**
 * Unary operations.
 */
public interface UnaryValue extends Value {
    Value getInput();

    default ValueType getType() {
        return getInput().getType();
    }

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInput() : Util.throwIndexOutOfBounds(index);
    }
}
