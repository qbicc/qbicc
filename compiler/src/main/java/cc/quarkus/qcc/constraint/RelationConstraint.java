package cc.quarkus.qcc.constraint;

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
