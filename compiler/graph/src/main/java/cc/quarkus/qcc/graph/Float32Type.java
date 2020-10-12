package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class Float32Type extends NativeObjectTypeImpl implements FloatType {
    private final Constraint constraint;

    Float32Type() {
        constraint = Constraint.greaterThanOrEqualTo(new ConstantValue32(Float.floatToIntBits(Float.MIN_VALUE), this)).union(Constraint.lessThanOrEqualTo(new ConstantValue32(Float.floatToIntBits(Float.MAX_VALUE), this)));
    }

    public int getSize() {
        return 4;
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

    public Object boxValue(final ConstantValue value) {
        return Float.valueOf(Float.intBitsToFloat(value.intValue()));
    }
}
