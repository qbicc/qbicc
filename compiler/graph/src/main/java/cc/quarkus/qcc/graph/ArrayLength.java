package cc.quarkus.qcc.graph;

/**
 * The length of a Java array instance.
 */
public final class ArrayLength extends AbstractValue implements InstanceOperation, Value {
    private final Value instance;

    ArrayLength(final Value instance) {
        this.instance = instance;
    }

    public Value getInstance() {
        return null;
    }

    public Type getType() {
        return Type.S32;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
