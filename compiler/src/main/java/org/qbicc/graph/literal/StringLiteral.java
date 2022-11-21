package org.qbicc.graph.literal;

import org.qbicc.graph.Value;
import org.qbicc.type.ReferenceType;

/**
 *
 */
public final class StringLiteral extends WordLiteral {
    private final ReferenceType type;
    private final String value;
    private final boolean latin1;

    StringLiteral(final ReferenceType type, final String value) {
        this.type = type;
        this.value = value;
        boolean latin1 = true;
        for (int i = 0; i < value.length(); i ++) {
            if (value.charAt(i) >= 0x100) {
                latin1 = false;
                break;
            }
        }
        this.latin1 = latin1;
    }

    public ReferenceType getType() {
        return type;
    }

    public boolean isLatin1() {
        return latin1;
    }

    public String getValue() {
        return value;
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isZero() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof StringLiteral && equals((StringLiteral) other);
    }

    public boolean equals(final StringLiteral other) {
        return this == other || other != null && type.equals(other.type) && value.equals(other.value);
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('"').append(value).append('"');
    }

    @Override
    public boolean isDefEq(Value other) {
        return equals(other);
    }

    @Override
    public boolean isDefNe(Value other) {
        return other instanceof StringLiteral && ! equals((StringLiteral) other);
    }
}
