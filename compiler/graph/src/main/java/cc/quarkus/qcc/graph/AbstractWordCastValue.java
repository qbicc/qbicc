package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;

abstract class AbstractWordCastValue extends AbstractValue implements WordCastValue {
    final Value value;
    final WordType toType;

    AbstractWordCastValue(final Value value, final WordType toType) {
        this.value = value;
        this.toType = toType;
    }

    public Value getInput() {
        return value;
    }

    public ValueType getType() {
        return toType;
    }
}
