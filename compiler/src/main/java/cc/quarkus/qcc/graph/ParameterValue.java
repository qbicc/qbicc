package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class ParameterValue extends AbstractValue {
    private final ValueType type;
    private final int index;

    ParameterValue(final Node callSite, final ExecutableElement element, final ValueType type, final int index) {
        super(callSite, element, 0, -1);
        this.type = type;
        this.index = index;
    }

    public ValueType getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(ParameterValue.class, type) * 19 + index;
    }

    public boolean equals(final Object other) {
        return other instanceof ParameterValue && equals((ParameterValue) other);
    }

    public boolean equals(final ParameterValue other) {
        return this == other || other != null
            && index == other.index
            && type.equals(other.type);
    }
}
