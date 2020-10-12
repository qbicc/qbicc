package cc.quarkus.qcc.graph;

final class ClassLiteralTypeImpl extends AbstractType implements ClassLiteralType {

    public ArrayClassType getArrayClassType() {
        throw new IllegalArgumentException("Convert to object type first");
    }
}
