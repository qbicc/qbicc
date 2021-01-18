package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ValueType;

public interface Value extends Node {

    ValueType getType();

    <T, R> R accept(ValueVisitor<T, R> visitor, T param);

    // static

    Value[] NO_VALUES = new Value[0];
}
