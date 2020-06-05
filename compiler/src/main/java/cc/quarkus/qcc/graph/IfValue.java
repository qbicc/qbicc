package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
public interface IfValue extends ProgramNode, Value {
    Value getCond();
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
}
