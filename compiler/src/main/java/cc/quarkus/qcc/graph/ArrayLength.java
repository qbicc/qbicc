package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * The length of a Java array instance.
 */
public final class ArrayLength extends AbstractValue {
    private final ValueHandle instance;
    private final SignedIntegerType type;

    ArrayLength(final Node callSite, final ExecutableElement element, final int line, final int bci, final ValueHandle instance, final SignedIntegerType type) {
        super(callSite, element, line, bci);
        this.instance = instance;
        this.type = type;
    }

    public ValueHandle getInstance() {
        return instance;
    }

    public SignedIntegerType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return instance;
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
