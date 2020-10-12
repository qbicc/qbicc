package cc.quarkus.qcc.graph;

final class ArrayTypeImpl extends AbstractType implements ArrayType {
    private final Type elementType;

    ArrayTypeImpl(final Type elementType) {
        this.elementType = elementType;
    }

    public Type getElementType() {
        return elementType;
    }

    public ArrayClassType getArrayClassType() {
        throw new UnsupportedOperationException();
    }
}
