package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.WordType;

abstract class AbstractWordCastValue extends AbstractValue implements WordCastValue {
    final Value value;
    final WordType toType;

    AbstractWordCastValue(final int line, final int bci, final Value value, final WordType toType) {
        super(line, bci);
        this.value = value;
        this.toType = toType;
    }

    public Value getInput() {
        return value;
    }

    public WordType getType() {
        return toType;
    }
}
