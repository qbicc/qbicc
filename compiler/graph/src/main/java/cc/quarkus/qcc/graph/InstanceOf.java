package cc.quarkus.qcc.graph;

/**
 *
 */
public final class InstanceOf extends AbstractValue implements InstanceOperation {
    private final Value instance;
    private final ClassType toType;

    InstanceOf(final Value instance, final ClassType toType) {
        this.instance = instance;
        this.toType = toType;
    }

    public Type getType() {
        return Type.BOOL;
    }

    public ClassType getToType() {
        return toType;
    }

    public Value getInstance() {
        return instance;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
