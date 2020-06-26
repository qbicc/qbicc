package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

final class PointerTypeImpl extends NativeObjectTypeImpl implements PointerType {
    private final NativeObjectType pointeeType;

    PointerTypeImpl(final NativeObjectType pointeeType) {
        this.pointeeType = pointeeType;
    }

    public NativeObjectType getPointeeType() {
        return pointeeType;
    }

    public int getSize() {
        throw new UnsupportedOperationException("Depends on platform...");
    }

    public ConstantValue bitCast(final ConstantValue other) {
        Type otherType = other.getType();
        if (otherType instanceof WordType) {
            WordType wordType = (WordType) otherType;
            int size = wordType.getSize();
            if (size >= getSize()) {
                if (getSize() == 4) {
                    return new ConstantValue32(other.intValue(), this);
                } else {
                    assert getSize() == 8;
                    return new ConstantValue64(other.longValue(), this);
                }
            }
        }
        throw new UnsupportedOperationException("Invalid cast operation");
    }

    public int getParameterCount() {
        return 0;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        return null;
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        return null;
    }

    public String getLabelForGraph() {
        return "pointer";
    }
}
