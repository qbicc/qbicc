package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.SignedIntegerType;

/**
 * The length of a Java array instance.
 */
public final class ArrayLength extends AbstractValue implements InstanceOperation {
    private final Value instance;
    private final SignedIntegerType type;

    ArrayLength(final int line, final int bci, final Value instance, final SignedIntegerType type) {
        super(line, bci);
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

    int calcHashCode() {
        return Objects.hash(ArrayLength.class, instance);
    }

    public boolean equals(final Object other) {
        return other instanceof ArrayLength && equals((ArrayLength) other);
    }

    public boolean equals(final ArrayLength other) {
        return this == other || other != null && instance.equals(other.instance);
    }
}
