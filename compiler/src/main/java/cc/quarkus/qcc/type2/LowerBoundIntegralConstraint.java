package cc.quarkus.qcc.type2;

import java.util.Optional;

import static cc.quarkus.qcc.type2.IntegralConstraint.Op.*;

class LowerBoundIntegralConstraint extends HalfIntegralConstraintImpl {

    LowerBoundIntegralConstraint(IntegralConstraint.Op op, long num) {
        super(op, num);
    }

    public LowerBoundIntegralConstraint join(LowerBoundIntegralConstraint other) {
        if ( other == null ) {
            return null;
        }

        if ( getBound() < other.getBound() ) {
            return this;
        } else if ( other.getBound() < getBound()) {
            return other;
        } else {
            if ( getOp() == other.getOp() ) {
                return this;
            }
            if ( this.getOp() == INCLUSIVE ) {
                return this;
            } else {
                return other;
            }
        }
    }

    public LowerBoundIntegralConstraint meet(LowerBoundIntegralConstraint other) {
        if ( other == null ) {
            return this;
        }

        if ( getBound() < other.getBound() ) {
            return other;
        } else if ( other.getBound() < getBound() ) {
            return this;
        } else {
            if ( getOp() == other.getOp() ) {
                return this;
            }
            if ( this.getOp() == EXCLUSIVE ) {
                return this;
            } else {
                return other;
            }
        }
    }

    @Override
    public String toString() {
        return "[Lower " + getOp() + " " + getBound() + "]";
    }
}
