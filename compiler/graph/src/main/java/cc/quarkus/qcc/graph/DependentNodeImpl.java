package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

abstract class DependentNodeImpl extends NodeImpl {
    private NodeHandle dependency;

    public int getBasicDependencyCount() {
        return dependency != null ? 1 : 0;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        NodeHandle dependency = this.dependency;
        if (dependency == null) {
            return super.getBasicDependency(index);
        }
        return NodeHandle.getTargetOf(dependency);
    }

    void setBasicDependency(final Node dependency) {
        this.dependency = NodeHandle.of(dependency);
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(dependency), "depends-on", "purple", "dotted", knownBlocks);
    }
}
