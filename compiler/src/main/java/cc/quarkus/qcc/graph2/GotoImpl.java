package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
final class GotoImpl extends TerminatorImpl implements Goto {
    NodeHandle target;

    GotoImpl() {
    }

    public BasicBlock getNextBlock() {
        return NodeHandle.getTargetOf(target);
    }

    public void setNextBlock(final BasicBlock branch) {
        target = NodeHandle.of(branch);
    }

    void setTarget(final NodeHandle target) {
        this.target = target;
    }

    public String getLabelForGraph() {
        return "goto";
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(target), "goes-to", "black", "solid", knownBlocks);
        return graph;
    }
}
