package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.WordType;

abstract class AbstractWordCastValue extends AbstractValue implements WordCastValue {
    final Value value;
    final WordType toType;

    AbstractWordCastValue(final ProgramLocatable pl, final Value value, final WordType toType) {
        super(pl);
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
        return other instanceof AbstractWordCastValue wc && equals(wc);
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
