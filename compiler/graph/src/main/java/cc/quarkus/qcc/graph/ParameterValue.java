package cc.quarkus.qcc.graph;

/**
 *
 */
public final class ParameterValue extends AbstractValue {
    private final Type type;
    private final int index;

    ParameterValue(final Type type, final int index) {
        this.type = type;
        this.index = index;
    }

    public Type getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
