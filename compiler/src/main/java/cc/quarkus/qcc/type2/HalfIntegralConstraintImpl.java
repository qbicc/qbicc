package cc.quarkus.qcc.type2;

import cc.quarkus.qcc.type2.IntegralConstraint.Op;

class HalfIntegralConstraintImpl implements HalfIntegralConstraint {

    HalfIntegralConstraintImpl(Op op, long bound) {
        this.op = op;
        this.bound = bound;
    }

    public Op getOp() {
        return this.op;
    }

    public long getBound() {
        return this.bound;
    }

    private final Op op;
    private final long bound;
}
