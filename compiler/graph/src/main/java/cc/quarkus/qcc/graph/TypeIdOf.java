package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.TypeIdType;
import cc.quarkus.qcc.type.ValueType;

/**
 * The type ID of a given value.
 */
public final class TypeIdOf extends AbstractValue implements InstanceOperation {
    private final TypeIdType type;
    private final Value instance;

    TypeIdOf(final int line, final int bci, final TypeIdType type, final Value instance) {
        super(line, bci);
        this.type = type;
        this.instance = instance;
    }

    public ValueType getType() {
        return type;
    }

    public Value getInstance() {
        return instance;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
