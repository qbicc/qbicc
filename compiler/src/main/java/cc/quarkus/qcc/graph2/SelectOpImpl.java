package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

final class SelectOpImpl extends OwnedValueImpl implements SelectOp {
    NodeHandle cond;
    NodeHandle ifTrue;
    NodeHandle ifFalse;

    public Value getCond() {
        return cond.getTarget();
    }

    public void setCond(final Value cond) {
        this.cond = NodeHandle.of(cond);
    }

    public Value getTrueValue() {
        return ifTrue.getTarget();
    }

    public void setTrueValue(final Value value) {
        ifTrue = NodeHandle.of(value);
    }

    public Value getFalseValue() {
        return ifFalse.getTarget();
    }

    public void setFalseValue(final Value value) {
        ifFalse = NodeHandle.of(value);
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, cond.getTarget(), "condition", "black", "solid", knownBlocks);
        addEdgeTo(visited, graph, ifTrue.getTarget(), "if-true", "green", "solid", knownBlocks);
        addEdgeTo(visited, graph, ifFalse.getTarget(), "if-false", "red", "solid", knownBlocks);
        return graph;
    }

    public String getLabelForGraph() {
        return "select";
    }
}
