package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A general parameter value.
 */
public final class ParameterValue extends AbstractValue implements Unschedulable {
    public static final ParameterValue[] NO_PARAMETER_VALUES = new ParameterValue[0];

    private final ValueType type;
    private final String label;
    private final int index;
    private final boolean nullable;

    ParameterValue(final Node callSite, final ExecutableElement element, final ValueType type, String label, final int index) {
        super(callSite, element, 0, -1);
        this.type = type;
        this.label = label;
        this.index = index;
        nullable = label.equals("p");
    }

    public ValueType getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(ParameterValue.class, type, label) * 19 + index;
    }

    @Override
    String getNodeName() {
        return "Parameter";
    }

    public boolean equals(final Object other) {
        return other instanceof ParameterValue && equals((ParameterValue) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        b.append(label);
        b.append(index);
        b.append(')');
        return b;
    }

    public boolean equals(final ParameterValue other) {
        return this == other || other != null
            && index == other.index
            && label.equals(other.label)
            && type.equals(other.type);
    }
}
