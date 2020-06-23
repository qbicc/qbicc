package cc.quarkus.qcc.graph;

final class ReturnImpl extends TerminatorImpl implements Return {
    public String getLabelForGraph() {
        return "return";
    }
}
