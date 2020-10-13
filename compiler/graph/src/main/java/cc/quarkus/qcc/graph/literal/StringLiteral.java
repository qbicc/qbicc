package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.StringType;
import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public final class StringLiteral extends Literal {
    private final StringType type;
    private final String value;
    private final boolean latin1;

    StringLiteral(final StringType type, final String value) {
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

    public ValueType getType() {
        return type;
    }

    public boolean isLatin1() {
        return latin1;
    }

    public String getValue() {
        return value;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean equals(final Literal other) {
        return other instanceof StringLiteral && equals((StringLiteral) other);
    }

    public boolean equals(final StringLiteral other) {
        return this == other || other != null && type.equals(other.type) && value.equals(other.value);
    }
}
