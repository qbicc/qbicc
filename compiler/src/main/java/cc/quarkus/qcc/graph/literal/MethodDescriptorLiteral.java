package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.MethodDescriptorType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

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
}
