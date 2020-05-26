package cc.quarkus.qcc.type2;

import static cc.quarkus.qcc.type2.IntegralConstraint.Op.*;

public interface IntegralConstraint {

    enum Op {
        INCLUSIVE,
        EXCLUSIVE,
    }

    IntegralConstraint TOP = new TopIntegralConstraint();
    IntegralConstraint BOTTOM = new BottomIntegralConstraint();

    static IntegralConstraint constrained(LowerBoundIntegralConstraint lower, UpperBoundIntegralConstraint upper) {
        return new IntegralConstraintImpl(lower,
                                          upper);
    }

    static IntegralConstraint greaterThan(long num) {
        return constrained(new LowerBoundIntegralConstraint(EXCLUSIVE, num),
                           null);
    }

    static IntegralConstraint greaterThanOrEqual(long num) {
        return constrained(new LowerBoundIntegralConstraint(INCLUSIVE, num),
                           null);
    }

    static IntegralConstraint lessThan(long num) {
        return constrained(null,
                           new UpperBoundIntegralConstraint(EXCLUSIVE, num));
    }

    static IntegralConstraint lessThanOrEqual(long num) {
        return constrained(null,
                           new UpperBoundIntegralConstraint(INCLUSIVE, num));
    }

    static IntegralConstraint equal(long num) {
        return constrained(
                new LowerBoundIntegralConstraint(INCLUSIVE, num),
                new UpperBoundIntegralConstraint(INCLUSIVE, num)
        );
    }

    IntegralConstraint join(IntegralConstraint other);
    IntegralConstraint meet(IntegralConstraint other);

    default LowerBoundIntegralConstraint getLower() {
        return null;
    }

    default UpperBoundIntegralConstraint getUpper() {
        return null;
    }

    class TopIntegralConstraint implements IntegralConstraint {
        @Override
        public IntegralConstraint join(IntegralConstraint other) {
            return this;
        }

        @Override
        public IntegralConstraint meet(IntegralConstraint other) {
            return other;
        }
    }

    class BottomIntegralConstraint implements IntegralConstraint {
        @Override
        public IntegralConstraint join(IntegralConstraint other) {
            return other;
        }

        @Override
        public IntegralConstraint meet(IntegralConstraint other) {
            return this;
        }
    }
}
