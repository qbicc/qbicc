package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.element.Element;

abstract class AbstractWordCastValue extends AbstractValue implements WordCastValue {
    final Value value;
    final WordType toType;

    AbstractWordCastValue(final Element element, final int line, final int bci, final Value value, final WordType toType) {
        super(element, line, bci);
        this.value = value;
        this.toType = toType;
    }

    public Value getInput() {
        return value;
    }

    public WordType getType() {
        return toType;
    }

    int calcHashCode() {
        return Objects.hash(getClass(), value, toType);
    }

    public boolean equals(final Object other) {
        return other.getClass() == getClass() && equals((AbstractWordCastValue) other);
    }

    boolean equals(AbstractWordCastValue other) {
        return this == other || value.equals(other.value) && toType.equals(other.toType);
    }
}
