package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public final class ParameterValue extends AbstractValue {
    private final ValueType type;
    private final int index;

    ParameterValue(final ValueType type, final int index) {
        super(0, -1);
        this.type = type;
        this.index = index;
    }

    public ValueType getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
