package cc.quarkus.qcc.constraint;

import cc.quarkus.qcc.graph.Value;

public interface RelationConstraint extends Constraint {

    enum Op {
        GT,
        GE,
        LT,
        LE,
        EQ,
        NE
    }

    Op getOp();
    Value getValue();
}
