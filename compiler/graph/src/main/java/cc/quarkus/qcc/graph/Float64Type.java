package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class Float64Type extends NativeObjectTypeImpl implements FloatType {
    private final Constraint constraint;

    Float64Type() {
        constraint = Constraint.greaterThanOrEqualTo(new ConstantValue64(Double.doubleToLongBits(Double.MIN_VALUE), this)).union(Constraint.lessThanOrEqualTo(new ConstantValue64(Double.doubleToLongBits(Double.MAX_VALUE), this)));
    }

    public boolean isClass2Type() {
        return true;
    }

    public Object boxValue(final ConstantValue value) {
        return Double.valueOf(Double.longBitsToDouble(value.longValue()));
    }

    public int getSize() {
        return 8;
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

    public int getParameterCount() {
        return 1;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        if (index == 0) {
            return "value";
        } else {
            throw new IndexOutOfBoundsException(index);
        }
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        if (index == 0) {
            return constraint;
        } else {
            throw new IndexOutOfBoundsException(index);
        }
    }

    public String getLabelForGraph() {
        return "float64";
    }
}
