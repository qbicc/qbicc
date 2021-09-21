package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;

public class FunctionParameterLiteral extends Literal {
    private final String name;
    private final PointerType type;

    FunctionParameterLiteral(String name, ValueType type) {
        this.name = name;
        this.type = type.getPointer();
    }

    @Override
    public ValueType getType() {
        return type;
    }

    public String getName() { return name; }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public boolean equals(Literal other) {
        return other instanceof FunctionParameterLiteral && equals((FunctionParameterLiteral) other);
    }

    public boolean equals(FunctionParameterLiteral other) {
        return other == this || other != null && name.equals(other.name) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 19 + type.hashCode();
    }
}
