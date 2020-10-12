package cc.quarkus.qcc.graph;

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
}
