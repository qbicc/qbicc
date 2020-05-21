package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

abstract class InstructionImpl extends NodeImpl implements Instruction {
    NodeHandle dependency;

    InstructionImpl() {}

    public int getLine() {
        return 0;
    }

    public boolean hasDependency() {
        return dependency != null;
    }

    public Instruction getDependency() {
        return dependency.getTarget();
    }

    public void setDependency(final Instruction dependency) {
        this.dependency = NodeHandle.of(dependency);
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph) throws IOException {
        super.writeToGraph(visited, graph);
        NodeHandle dependency = this.dependency;
        if (dependency != null) {
            addEdgeTo(visited, graph, dependency.getTarget(), "depends-on", "black", "solid");
        }
        return graph;
    }
}

