package cc.quarkus.qcc.graph2;

final class ReturnImpl extends TerminatorImpl implements Return {
    public String getLabelForGraph() {
        return "return";
    }
}
