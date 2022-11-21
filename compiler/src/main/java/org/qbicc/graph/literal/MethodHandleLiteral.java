package org.qbicc.graph.literal;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.methodhandle.MethodHandleConstant;

/**
 * A literal representing a method handle.
 */
public final class MethodHandleLiteral extends Literal {
    private final MethodHandleConstant methodHandleConstant;
    private final ReferenceType type;

    MethodHandleLiteral(MethodHandleConstant methodHandleConstant, ReferenceType type) {
        this.methodHandleConstant = Assert.checkNotNullParam("methodHandleConstant", methodHandleConstant);
        this.type = Assert.checkNotNullParam("type", type);
    }

    public boolean isZero() {
        return false;
    }

    public MethodHandleConstant getMethodHandleConstant() {
        return methodHandleConstant;
    }

    public boolean equals(final Literal other) {
        return other instanceof MethodHandleLiteral && equals((MethodHandleLiteral) other);
    }

    public boolean equals(final MethodHandleLiteral other) {
        return this == other || other != null && methodHandleConstant.equals(other.methodHandleConstant);
    }

    public int hashCode() {
        return methodHandleConstant.hashCode();
    }

    public ReferenceType getType() {
      return type;
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append("methodhandle");
    }
}
