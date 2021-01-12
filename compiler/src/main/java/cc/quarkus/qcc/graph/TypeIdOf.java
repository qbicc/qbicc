package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.TypeType;

/**
 * The type ID of a given value.
 */
public final class TypeIdOf extends AbstractValue implements InstanceOperation {
    private final Value instance;

    TypeIdOf(final int line, final int bci, final Value instance) {
        super(line, bci);
        this.instance = instance;
    }

    public TypeType getType() {
        return instance.getType().getTypeType();
    }

    public Value getInstance() {
        return instance;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(TypeIdOf.class, instance);
    }

    public boolean equals(final Object other) {
        return other instanceof TypeIdOf && equals((TypeIdOf) other);
    }

    public boolean equals(final TypeIdOf other) {
        return this == other || other != null
            && instance.equals(other.instance);
    }
}
