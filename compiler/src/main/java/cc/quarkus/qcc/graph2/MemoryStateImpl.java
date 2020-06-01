package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

abstract class MemoryStateImpl extends ProgramNodeImpl implements MemoryState {
    private NodeHandle memoryDependency;

    public MemoryState getMemoryDependency() {
        return NodeHandle.getTargetOf(memoryDependency);
    }

    public void setMemoryDependency(final MemoryState memoryState) {
        memoryDependency = NodeHandle.of(memoryState);
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(memoryDependency), "depends-on", "purple", "dotted", knownBlocks);
    }
}
