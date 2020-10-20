package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public final class ThisValue extends AbstractValue {
    private final ReferenceType type;

    ThisValue(final ReferenceType type) {
        super(0, -1);
        this.type = type;
    }

    public ValueType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
