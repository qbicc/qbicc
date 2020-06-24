package cc.quarkus.qcc.graph;

final class ThrowImpl extends TerminatorImpl implements Throw {
    NodeHandle thrownValue;

    public Value getThrownValue() {
        return NodeHandle.getTargetOf(thrownValue);
    }

    public void setThrownValue(final Value thrown) {
        thrownValue = NodeHandle.of(thrown);
    }

    public String getLabelForGraph() {
        return "throw";
    }
}
