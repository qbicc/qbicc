package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
final class StaticFieldWriteImpl extends FieldOperationImpl implements FieldWrite {
    NodeHandle writeValue;

    public Value getWriteValue() {
        return NodeHandle.getTargetOf(writeValue);
    }

    public void setWriteValue(final Value value) {
        writeValue = NodeHandle.of(value);
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, this, "write-value", "black", "solid", knownBlocks);
        return graph;
    }

    public String getLabelForGraph() {
        return "put-static-field";
    }
}
