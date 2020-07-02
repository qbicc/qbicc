package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

final class UnaryValueImpl extends ValueImpl implements UnaryValue {
    NodeHandle input;
    Kind kind;

    public Value getInput() {
        return NodeHandle.getTargetOf(input);
    }

    public void setInput(final Value input) {
        this.input = NodeHandle.of(input);
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(final Kind kind) {
        this.kind = kind;
    }

    public String getLabelForGraph() {
        return kind.toString();
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, this, "input", "black", "solid", knownBlocks);
    }

    String getShape() {
        return "oval";
    }
}
