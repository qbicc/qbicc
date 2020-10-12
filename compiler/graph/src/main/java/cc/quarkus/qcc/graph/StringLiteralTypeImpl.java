package cc.quarkus.qcc.graph;

final class StringLiteralTypeImpl extends AbstractType implements StringLiteralType {

    public ArrayClassType getArrayClassType() {
        throw new IllegalArgumentException("Convert to object type first");
    }
}
