package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

public interface Value extends Node {
    Constraint getConstraint();

    void setConstraint(Constraint constraint);

    Value[] NO_VALUES = new Value[0];

    Value ICONST_0 = iconst(0);
    Value LCONST_0 = lconst(0);

    static Value iconst(int operand) {
        // todo: cache
        return new IntConstantValueImpl(operand, Type.S32);
    }

    static Value lconst(long operand) {
        // todo: cache
        return new LongConstantValueImpl(operand, Type.S64);
    }
}
