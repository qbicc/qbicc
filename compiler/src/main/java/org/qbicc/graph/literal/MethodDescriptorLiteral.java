package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.MethodDescriptorType;
import org.qbicc.type.ValueType;

/**
 * A literal representing a method handle.
 */
public final class MethodDescriptorLiteral extends Literal {
    private final MethodDescriptorType type;
    private final String desc;

    MethodDescriptorLiteral(MethodDescriptorType type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public boolean isZero() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof MethodDescriptorLiteral && equals((MethodDescriptorLiteral) other);
    }

    public boolean equals(final MethodDescriptorLiteral other) {
        return this == other || other != null && desc.equals(other.desc);
    }

    public int hashCode() {
        return type.hashCode() * 19 + desc.hashCode();
    }

    public ValueType getType() {
      return this.type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append("descriptor").append('(').append(desc).append(')');
    }
}
