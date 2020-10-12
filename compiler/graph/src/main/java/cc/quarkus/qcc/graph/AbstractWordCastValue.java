package cc.quarkus.qcc.graph;

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

    public WordType getType() {
        return toType;
    }
}
