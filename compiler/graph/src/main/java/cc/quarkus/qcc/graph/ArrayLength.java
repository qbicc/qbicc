package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.SignedIntegerType;

/**
 * The length of a Java array instance.
 */
public final class ArrayLength extends AbstractValue implements InstanceOperation {
    private final Value instance;
    private final SignedIntegerType type;

    ArrayLength(final Value instance, final SignedIntegerType type) {
        this.instance = instance;
        this.type = type;
    }

    public Value getInstance() {
        return instance;
    }

    public SignedIntegerType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
