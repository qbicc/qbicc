package org.qbicc.graph;

import org.qbicc.type.ValueType;

public interface Value extends Node {

    ValueType getType();

    <T, R> R accept(ValueVisitor<T, R> visitor, T param);

    // static

    Value[] NO_VALUES = new Value[0];

    default boolean isDefEq(Value other) {
        return equals(other);
    }

    default boolean isDefNe(Value other) {
        return false;
    }

    default boolean isDefLt(Value other) {
        return false;
    }

    default boolean isDefGt(Value other) {
        return false;
    }

    default boolean isDefLe(Value other) {
        return equals(other);
    }

    default boolean isDefGe(Value other) {
        return equals(other);
    }

    default boolean isDefNaN() {
        return false;
    }

    default boolean isDefNotNaN() {
        return false;
    }
}
