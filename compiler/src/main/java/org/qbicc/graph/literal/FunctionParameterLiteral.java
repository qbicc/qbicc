package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

public class FunctionParameterLiteral extends Literal {
    private final String name;
    private final PointerType type;
    private final ExecutableElement ee;
    private final ExecutableElement oe;
    FunctionParameterLiteral(String name, ValueType type, ExecutableElement ee, ExecutableElement oe) {
        this.name = name;
        this.type = type.getPointer();
        this.ee = ee;
        this.oe = oe;
    }

    @Override
    public ValueType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public ExecutableElement getExecutable() {
        return ee;
    }

    public ExecutableElement getOriginalElement() {
        return oe;
    }

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
        return other == this || other != null && name.equals(other.name)
            && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 19 + type.hashCode();
    }
}
