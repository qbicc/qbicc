package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class BooleanTypeImpl extends NativeObjectTypeImpl implements BooleanType {
    final ConstantValue false_ = new ConstantValue32(0, this);
    final ConstantValue true_ = new ConstantValue32(1, this);
    private final Constraint constraint = Constraint.greaterThanOrEqualTo(false_).intersect(Constraint.lessThanOrEqualTo(true_));

    public int getSize() {
        return 1;
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
        return value.intValue() > 0 ? Boolean.TRUE : Boolean.FALSE;
    }
}
