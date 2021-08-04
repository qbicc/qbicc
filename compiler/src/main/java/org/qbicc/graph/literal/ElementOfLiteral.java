package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.ValueType;

public class ElementOfLiteral extends Literal {
    final Literal value;
    final Literal index;

    public ElementOfLiteral(Literal value, Literal index) {
        this.value = value;
        this.index = index;
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public boolean equals(Literal other) {
        return other instanceof ElementOfLiteral && equals((ElementOfLiteral) other);
    }

    public boolean equals(ElementOfLiteral other) {
        return other == this || other != null && value.equals(other.value) && index.equals(other.index);
    }

    @Override
    public int hashCode() { return value.hashCode() * 19 + index.hashCode(); }

    @Override
    public ValueType getType() {
        return value.getType();
    }

    public Literal getValue() { return value; }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public Literal getIndex() {
        return index;
    }

    public String toString() {
        return "element_of ("+value+", "+index+")";
    }
}
