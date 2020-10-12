package cc.quarkus.qcc.graph;

/**
 *
 */
public final class ThisValue extends AbstractValue {
    private final ClassType type;

    ThisValue(final ClassType type) {
        this.type = type;
    }

    public ClassType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
