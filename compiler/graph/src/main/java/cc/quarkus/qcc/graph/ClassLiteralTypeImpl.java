package cc.quarkus.qcc.graph;

final class ClassLiteralTypeImpl extends NodeImpl implements ClassLiteralType {

    public String getLabelForGraph() {
        return "class literal";
    }

    public int getIdForGraph() {
        return 0;
    }

    public void setIdForGraph(final int id) {

    }
}
