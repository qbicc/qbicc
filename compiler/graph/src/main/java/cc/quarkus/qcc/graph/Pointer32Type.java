package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

final class Pointer32Type extends NativeObjectTypeImpl implements PointerType {
    private final NativeObjectType pointeeType;

    Pointer32Type(final NativeObjectType pointeeType) {
        this.pointeeType = pointeeType;
    }

    public NativeObjectType getPointeeType() {
        return pointeeType;
    }

    public int getSize() {
        return 4;
    }

    public ConstantValue bitCast(final ConstantValue other) {
        Type otherType = other.getType();
        if (otherType instanceof WordType) {
            WordType wordType = (WordType) otherType;
            int size = wordType.getSize();
            if (size >= getSize()) {
                return new ConstantValue32(other.intValue(), this);
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
        return "pointer32";
    }
}
