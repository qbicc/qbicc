package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

abstract class ProgramNodeImpl extends NodeImpl implements ProgramNode {
    private NodeHandle owner;
    private int sourceLine;

    public int getSourceLine() {
        return sourceLine;
    }

    public void setSourceLine(final int line) {
        this.sourceLine = line;
    }

    public BasicBlock getOwner() {
        return NodeHandle.getTargetOf(owner);
    }

    public void setOwner(final BasicBlock owner) {
        this.owner = NodeHandle.of(owner);
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, getOwner(), "owned-by", "brown", "dotted", knownBlocks);
    }
}
