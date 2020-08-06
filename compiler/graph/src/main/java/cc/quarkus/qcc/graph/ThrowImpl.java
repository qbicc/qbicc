package cc.quarkus.qcc.graph;

class ThrowImpl extends TerminatorImpl implements Throw {
    NodeHandle thrownValue;

    public Value getThrownValue() {
        return NodeHandle.getTargetOf(thrownValue);
    }

    public void setThrownValue(final Value thrown) {
        thrownValue = NodeHandle.of(thrown);
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public String getLabelForGraph() {
        return "throw";
    }
}
