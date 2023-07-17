package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.ValueType;

/**
 *
 */
public final class Select extends AbstractValue {
    private final Value condition;
    private final Value trueValue;
    private final Value falseValue;

    Select(final ProgramLocatable pl, final Value condition, final Value trueValue, final Value falseValue) {
        super(pl);
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

    public boolean isNullable() {
        return trueValue.isNullable() || falseValue.isNullable();
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
        condition.toReferenceString(b);
        b.append('?');
        trueValue.toReferenceString(b);
        b.append(':');
        falseValue.toReferenceString(b);
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
