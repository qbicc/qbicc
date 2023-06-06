package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ExecutableElement;

abstract class AbstractWordCastValue extends AbstractValue implements WordCastValue {
    final Value value;
    final WordType toType;

    AbstractWordCastValue(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value value, final WordType toType) {
        super(callSite, element, line, bci);
        this.value = value;
        this.toType = toType;
    }

    public Value getInput() {
        return value;
    }

    public WordType getType() {
        return toType;
    }

    @Override
    public WordType getInputType() {
        return getInput().getType(WordType.class);
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

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        getInput().toReferenceString(b);
        b.append(')');
        b.append(" to ");
        toType.toString(b);
        return b;
    }
}
