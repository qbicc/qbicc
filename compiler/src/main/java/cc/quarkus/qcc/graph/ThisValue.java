package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class ThisValue extends AbstractValue implements Unschedulable {
    private final ReferenceType type;

    ThisValue(final Node callSite, final ExecutableElement element, final ReferenceType type) {
        super(callSite, element, 0, -1);
        this.type = type;
    }

    public ValueType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(ThisValue.class, type);
    }

    public boolean equals(final Object other) {
        return other instanceof ThisValue && equals((ThisValue) other);
    }

    public boolean equals(final ThisValue other) {
        return this == other || other != null
            && type.equals(other.type);
    }
}
