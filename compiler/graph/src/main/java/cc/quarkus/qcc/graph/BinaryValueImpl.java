package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
abstract class BinaryValueImpl extends ValueProgramNodeImpl implements BinaryValue {
    NodeHandle left;
    NodeHandle right;

    public Value getLeftInput() {
        return NodeHandle.getTargetOf(left);
    }

    public void setLeftInput(final Value value) {
        left = NodeHandle.of(value);
    }

    public Value getRightInput() {
        return NodeHandle.getTargetOf(right);
    }

    public void setRightInput(final Value value) {
        right = NodeHandle.of(value);
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(left), "left-input", "black", "solid", knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(right), "right-input", "black", "solid", knownBlocks);
    }
}
