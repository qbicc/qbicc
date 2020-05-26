package cc.quarkus.qcc.type2;

import static cc.quarkus.qcc.type2.IntegralConstraint.Op.*;

class UpperBoundIntegralConstraint extends HalfIntegralConstraintImpl {

    UpperBoundIntegralConstraint(IntegralConstraint.Op op, long num) {
        super(op, num);
    }

    UpperBoundIntegralConstraint join(UpperBoundIntegralConstraint other) {
        if ( other == null ) {
            return null;
        }

        if ( getBound()> other.getBound() ) {
            return this;
        } else if ( other.getBound() > getBound()) {
            return other;
        } else {
            if ( getOp() == other.getOp() ) {
                return this;
            }
            if ( getOp() == INCLUSIVE ) {
                return this;
            } else {
                return other;
            }
        }
    }

    UpperBoundIntegralConstraint meet(UpperBoundIntegralConstraint other) {
        if ( other == null ) {
            return this;
        }

        if ( getBound() > other.getBound() ) {
            return other;
        } else if ( other.getBound() > getBound() ) {
            return this;
        } else {
            if ( getOp() == other.getOp() ) {
                return this;
            }
            if ( getOp() == EXCLUSIVE ) {
                return this;
            } else {
                return other;
            }
        }
    }

    @Override
    public String toString() {
        return "[Upper " + getOp() + " " + getBound() + "]";
    }
}
