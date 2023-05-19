package org.qbicc.graph.literal;

import org.qbicc.graph.Value;
import org.qbicc.type.ArrayType;
import org.qbicc.type.StructType;
import org.qbicc.type.ValueType;

/**
 * A zero-initializer literal of some type which can be used to initialize arrays and structures.
 */
public final class ZeroInitializerLiteral extends Literal {
    private final ValueType type;

    ZeroInitializerLiteral(final ValueType type) {
        this.type = type;
    }

    public ValueType getType() {
        return type;
    }

    @Override
    public boolean isZero() {
        return true;
    }

    public boolean equals(final Literal other) {
        return other instanceof ZeroInitializerLiteral && equals((ZeroInitializerLiteral) other);
    }

    public boolean equals(final ZeroInitializerLiteral other) {
        return other == this || other != null && type.equals(other.type);
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public Value extractElement(LiteralFactory lf, final Value index) {
        if (type instanceof ArrayType at) {
            return lf.zeroInitializerLiteralOfType(at.getElementType());
        }
        return null;
    }

    public Value extractMember(LiteralFactory lf, StructType.Member member) {
        if (type instanceof StructType) {
            return lf.zeroInitializerLiteralOfType(member.getType());
        }
        return null;
    }

    public int hashCode() {
        return ZeroInitializerLiteral.class.hashCode();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append('(');
        type.toString(b);
        b.append(')');
        b.append("{0}");
        return b;
    }
}
