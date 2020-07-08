package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
public interface IfValue extends Value {
    Value getCondition();
    void setCond(Value cond);
    Value getTrueValue();
    void setTrueValue(Value value);
    Value getFalseValue();
    void setFalseValue(Value value);

    default Type getType() {
        return getTrueValue().getType();
    }

    default Constraint getConstraint() {
        // todo: cache?
        return getTrueValue().getConstraint().union(getFalseValue().getConstraint());
    }

    default int getValueDependencyCount() {
        return 3;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getCondition() : index == 1 ? getTrueValue() : index == 2 ? getFalseValue() : Util.throwIndexOutOfBounds(index);
    }
}
