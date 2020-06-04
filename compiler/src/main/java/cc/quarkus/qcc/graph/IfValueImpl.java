package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

final class IfValueImpl extends ProgramNodeImpl implements IfValue {
    NodeHandle cond;
    NodeHandle ifTrue;
    NodeHandle ifFalse;

    public Value getCond() {
        return NodeHandle.getTargetOf(cond);
    }

    public void setCond(final Value cond) {
        this.cond = NodeHandle.of(cond);
    }

    public Value getTrueValue() {
        return NodeHandle.getTargetOf(ifTrue);
    }

    public void setTrueValue(final Value value) {
        ifTrue = NodeHandle.of(value);
    }

    public Value getFalseValue() {
        return NodeHandle.getTargetOf(ifFalse);
    }

    public void setFalseValue(final Value value) {
        ifFalse = NodeHandle.of(value);
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(cond), "condition", "black", "solid", knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(ifTrue), "if-true", "green", "solid", knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(ifFalse), "if-false", "red", "solid", knownBlocks);
    }

    public String getLabelForGraph() {
        return "select";
    }
}
