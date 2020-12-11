package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.PoisonType;

/**
 * The undefined value.  Usage of an undefined value results in a compilation error.
 */
public final class UndefinedLiteral extends Literal {
    private final PoisonType type;

    UndefinedLiteral(final PoisonType type) {
        this.type = type;
    }

    public boolean equals(final Literal other) {
        return other instanceof UndefinedLiteral;
    }

    public PoisonType getType() {
        return type;
    }

    public int hashCode() {
        return type.hashCode();
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
