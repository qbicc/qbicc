package cc.quarkus.qcc.graph;

/**
 *
 */
public final class Select extends AbstractValue {
    private final Value condition;
    private final Value trueValue;
    private final Value falseValue;

    Select(final Value condition, final Value trueValue, final Value falseValue) {
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

    public Type getType() {
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
}
