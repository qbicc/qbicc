package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.Element;

/**
 *
 */
public final class Select extends AbstractValue {
    private final Value condition;
    private final Value trueValue;
    private final Value falseValue;

    Select(final Element element, final int line, final int bci, final Value condition, final Value trueValue, final Value falseValue) {
        super(element, line, bci);
        this.condition = condition;
        this.trueValue = trueValue;
        this.falseValue = falseValue;
    }

    public Value getCondition() {
        return condition;
    }

    public Value getTrueValue() {
        return trueValue;
    }

    public Value getFalseValue() {
        return falseValue;
    }

    public ValueType getType() {
        return getTrueValue().getType();
    }

    public int getValueDependencyCount() {
        return 3;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getCondition() : index == 1 ? getTrueValue() : index == 2 ? getFalseValue() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(Select.class, condition, trueValue, falseValue);
    }

    public boolean equals(final Object other) {
        return other instanceof Select && equals((Select) other);
    }

    public boolean equals(final Select other) {
        return this == other || other != null
            && condition.equals(other.condition)
            && trueValue.equals(other.trueValue)
            && falseValue.equals(other.falseValue);
    }
}
