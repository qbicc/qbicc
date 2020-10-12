package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
abstract class SignedIntegerTypeImpl extends NativeObjectTypeImpl implements SignedIntegerType {
    private final Constraint constraint;

    SignedIntegerTypeImpl(final int minValue, final int maxValue) {
        constraint = Constraint.greaterThanOrEqualTo(new ConstantValue32(minValue, this)).intersect(Constraint.lessThanOrEqualTo(new ConstantValue32(maxValue, this)));
    }

    SignedIntegerTypeImpl(final long minValue, final long maxValue) {
        constraint = Constraint.greaterThanOrEqualTo(new ConstantValue64(minValue, this)).intersect(Constraint.lessThanOrEqualTo(new ConstantValue64(maxValue, this)));
    }

    public ConstantValue bitCast(final ConstantValue other) {
        Type otherType = other.getType();
        if (otherType instanceof WordType) {
            WordType otherWordType = (WordType) otherType;
            if (getSize() == otherWordType.getSize()) {
                return other.withTypeRaw(this);
            }
        }
        throw new IllegalArgumentException("Cannot bitcast from " + other + " to " + this);
    }
}
