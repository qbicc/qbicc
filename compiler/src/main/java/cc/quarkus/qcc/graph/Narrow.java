package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ReferenceType;

/**
 * A narrowed value.  The input value is assumed to be wider; violating this assumption can cause problems.
 */
public final class Narrow extends AbstractValue implements CastValue {
    private final Value input;
    private final ReferenceType type;

    Narrow(final int line, final int bci, final Value input, final ReferenceType type) {
        super(line, bci);
        this.input = input;
        this.type = type;
    }

    public Value getInput() {
        return input;
    }

    public ReferenceType getType() {
        return type;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? input : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(Narrow.class, input, type);
    }

    public boolean equals(final Object other) {
        return other instanceof Narrow && equals((Narrow) other);
    }

    public boolean equals(final Narrow other) {
        return this == other || other != null
            && input.equals(other.input)
            && type.equals(other.type);
    }
}
