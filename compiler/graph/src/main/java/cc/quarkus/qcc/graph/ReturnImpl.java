package cc.quarkus.qcc.graph;

final class ReturnImpl extends TerminatorImpl implements Return {

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public String getLabelForGraph() {
        return "return";
    }
}
