package org.qbicc.graph;

import org.qbicc.type.ValueType;

/**
 *
 */
public interface CastValue extends Value {
    Value getInput();

    ValueType getType();

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInput() : Util.throwIndexOutOfBounds(index);
    }

    @Override
    default boolean isNullable() {
        // no cast can introduce or discard nullability
        return getInput().isNullable();
    }

    default boolean isConstant() {
        return getInput().isConstant();
    }
}
