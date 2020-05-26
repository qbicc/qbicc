package cc.quarkus.qcc.type2;

import java.util.Optional;

import io.smallrye.common.constraint.Assert;

class IntegralConstraintImpl implements IntegralConstraint {

    IntegralConstraintImpl(LowerBoundIntegralConstraint lower, UpperBoundIntegralConstraint upper) {
        this.lower = lower;
        this.upper = upper;
    }

    public LowerBoundIntegralConstraint getLower() {
        return this.lower;
    }

    public UpperBoundIntegralConstraint getUpper() {
        return this.upper;
    }

    @Override
    public IntegralConstraint join(IntegralConstraint other) {
        if ( other == TOP ) {
            return other;
        }
        if ( other == BOTTOM ) {
            return this;
        }

        LowerBoundIntegralConstraint lb = getLower();
        UpperBoundIntegralConstraint ub = getUpper();

        LowerBoundIntegralConstraint olb = other.getLower();
        UpperBoundIntegralConstraint oub = other.getUpper();

        LowerBoundIntegralConstraint nlb = null;
        UpperBoundIntegralConstraint nub = null;

        if ( lb == null && olb == null ) {
            nlb = null;
        } else if ( olb == null ) {
            nlb = lb;
        } else if ( lb == null ) {
            nlb = olb;
        } else {
            nlb = lb.join(olb);
        }

        if ( ub == null && oub == null ) {
            nub = null;
        } else if ( oub == null ) {
            nub = ub;
        } else if ( ub == null ) {
            nub = oub;
        } else {
            nub = ub.join(oub);
        }

        if ( nlb == null && nub == null ) {
            return TOP;
        }

        if ( nlb != null && nub != null ) {
            if ( nlb.getBound() >= nub.getBound() ) {
                if ( nlb.getOp() != Op.INCLUSIVE && nub.getOp() != Op.INCLUSIVE ) {
                    return BOTTOM;
                }
            }
            if (nlb.getBound() == nub.getBound()) {
                if (nlb.getOp() == nub.getOp()) {
                    if (nlb.getOp() != Op.INCLUSIVE) {
                        return BOTTOM;
                    }
                }
            }
        }

        return new IntegralConstraintImpl(nlb, nub);
    }

    @Override
    public IntegralConstraint meet(IntegralConstraint other) {
        if ( other == TOP ) {
            return this;
        }
        if ( other == BOTTOM ) {
            return other;
        }

        LowerBoundIntegralConstraint lb = getLower();
        LowerBoundIntegralConstraint olb = other.getLower();

        UpperBoundIntegralConstraint ub = getUpper();
        UpperBoundIntegralConstraint oub = other.getUpper();

        LowerBoundIntegralConstraint nlb = null;
        UpperBoundIntegralConstraint nub = null;

        if ( lb == null && olb == null ) {
            nlb = null;
        } else if ( olb == null ) {
            nlb = lb;
        } else if ( lb == null ) {
            nlb = olb;
        } else {
            nlb = lb.meet(olb);
        }

        if ( ub == null && oub == null ) {
            nub = null;
        } else if ( oub == null ) {
            nub = ub;
        } else if ( ub == null ) {
            nub = oub;
        } else {
            nub = ub.meet(oub);
        }

        if ( nlb == null && nub == null ) {
            return TOP;
        }

        if ( nlb != null && nub != null ) {
            if ( nlb.getBound() > nub.getBound() ) {
                return BOTTOM;
            }
            if ( nlb.getBound() == nub.getBound() ) {
                if ( nlb.getOp() != Op.INCLUSIVE && nub.getOp() != Op.INCLUSIVE ) {
                    return BOTTOM;
                }
            }
            if (nlb.getBound() == nub.getBound()) {
                if (nlb.getOp() == nub.getOp()) {
                    if (nlb.getOp() != Op.INCLUSIVE) {
                        return BOTTOM;
                    }
                }
            }
        }

        return new IntegralConstraintImpl(nlb, nub);
    }


    @Override
    public String toString() {
        return "FullIntegralConstraint{" +
                "lower=" + lower +
                ", upper=" + upper +
                '}';
    }

    private final LowerBoundIntegralConstraint lower;
    private final UpperBoundIntegralConstraint upper;
}
