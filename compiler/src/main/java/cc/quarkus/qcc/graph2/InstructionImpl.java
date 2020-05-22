package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

abstract class InstructionImpl extends NodeImpl implements Instruction {
    NodeHandle dependency;

    InstructionImpl() {}

    public int getLine() {
        return 0;
    }

    public Instruction getDependency() {
        return dependency == null ? null : dependency.getTarget();
    }

    public void setDependency(final Instruction dependency) {
        this.dependency = NodeHandle.of(dependency);
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        NodeHandle dependency = this.dependency;
        if (dependency != null) {
            addEdgeTo(visited, graph, dependency.getTarget(), "depends-on", "black", "solid", knownBlocks);
        }
        return graph;
    }
}

