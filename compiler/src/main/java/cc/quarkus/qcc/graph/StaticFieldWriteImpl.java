package cc.quarkus.qcc.graph;

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

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, this, "write-value", "black", "solid", knownBlocks);
    }

    public String getLabelForGraph() {
        return "put-static-field";
    }
}
