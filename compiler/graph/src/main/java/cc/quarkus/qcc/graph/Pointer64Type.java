package cc.quarkus.qcc.graph;

final class Pointer64Type extends NativeObjectTypeImpl implements PointerType {
    private final NativeObjectType pointeeType;

    Pointer64Type(final NativeObjectType pointeeType) {
        this.pointeeType = pointeeType;
    }

    public NativeObjectType getPointeeType() {
        return pointeeType;
    }

    public int getSize() {
        return 8;
    }

    public ConstantValue bitCast(final ConstantValue other) {
        Type otherType = other.getType();
        if (otherType instanceof WordType) {
            WordType wordType = (WordType) otherType;
            int size = wordType.getSize();
            if (size >= getSize()) {
                return new ConstantValue64(other.longValue(), this);
            }
        }
        throw new UnsupportedOperationException("Invalid cast operation");
    }
}
