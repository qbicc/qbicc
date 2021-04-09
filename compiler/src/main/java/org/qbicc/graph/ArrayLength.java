package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * The length of a Java array instance.
 */
public final class ArrayLength extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final ValueHandle instance;
    private final SignedIntegerType type;

    ArrayLength(final Node callSite, final ExecutableElement element, final int line, final int bci, Node dependency, final ValueHandle instance, final SignedIntegerType type) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.instance = instance;
        this.type = type;
    }

    public Node getDependency() {
        return dependency;
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
        return Objects.hash(ArrayLength.class, instance, dependency);
    }

    public boolean equals(final Object other) {
        return other instanceof ArrayLength && equals((ArrayLength) other);
    }

    public boolean equals(final ArrayLength other) {
        return this == other || other != null && instance.equals(other.instance) && dependency.equals(other.dependency);
    }
}
