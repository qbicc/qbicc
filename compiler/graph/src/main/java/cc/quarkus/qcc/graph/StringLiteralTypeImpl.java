package cc.quarkus.qcc.graph;

final class StringLiteralTypeImpl extends NodeImpl implements StringLiteralType {

    public String getLabelForGraph() {
        return "string literal";
    }

    public int getIdForGraph() {
        return 0;
    }

    public void setIdForGraph(final int id) {

    }

    public ArrayClassType getArrayClassType() {
        throw new IllegalArgumentException("Convert to object type first");
    }
}
