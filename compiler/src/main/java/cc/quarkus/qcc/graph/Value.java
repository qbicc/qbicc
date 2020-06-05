package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

public interface Value extends Node {
    Constraint getConstraint();

    Type getType();

    void setConstraint(Constraint constraint);

    Value[] NO_VALUES = new Value[0];

    Value ICONST_0 = iconst(0);
    Value LCONST_0 = lconst(0);

    static Value iconst(int operand) {
        // todo: cache
        return new ConstantValue32(operand, Type.S32);
    }

    static Value lconst(long operand) {
        // todo: cache
        return new ConstantValue64(operand, Type.S64);
    }
}
