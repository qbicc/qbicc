package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class Select extends AbstractValue {
    private final Value condition;
    private final Value trueValue;
    private final Value falseValue;

    Select(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value condition, final Value trueValue, final Value falseValue) {
        super(callSite, element, line, bci);
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

    public boolean isConstant() {
        return condition.isConstant() && trueValue.isConstant() && falseValue.isConstant();
    }

    int calcHashCode() {
        return Objects.hash(Select.class, condition, trueValue, falseValue);
    }

    @Override
    String getNodeName() {
        return "Select";
    }

    public boolean equals(final Object other) {
        return other instanceof Select && equals((Select) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        condition.toString(b);
        b.append(',');
        trueValue.toString(b);
        b.append(',');
        falseValue.toString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final Select other) {
        return this == other || other != null
            && condition.equals(other.condition)
            && trueValue.equals(other.trueValue)
            && falseValue.equals(other.falseValue);
    }
}
