package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * A narrowed value.  The input value is assumed to be wider; violating this assumption can cause problems.
 */
public final class Narrow extends AbstractValue implements CastValue {
    private final Value input;
    private final ValueType type;

    Narrow(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value input, final ValueType type) {
        super(callSite, element, line, bci);
        this.input = input;
        ValueType inputType = input.getType();
        if (type instanceof ReferenceType && inputType instanceof ReferenceType) {
            this.type = ((ReferenceType) type).meet((ReferenceType) inputType);
        } else {
            this.type = type;
        }
    }

    public Value getInput() {
        return input;
    }

    public ValueType getType() {
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
