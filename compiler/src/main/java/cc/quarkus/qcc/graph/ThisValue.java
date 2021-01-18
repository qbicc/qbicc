package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.Element;

/**
 *
 */
public final class ThisValue extends AbstractValue {
    private final ReferenceType type;

    ThisValue(final Element element, final ReferenceType type) {
        super(element, 0, -1);
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
