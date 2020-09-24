package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
final class GotoImpl extends TerminatorImpl implements Goto {
    NodeHandle target;

    GotoImpl() {
    }

    public BasicBlock getTarget() {
        return NodeHandle.getTargetOf(target);
    }

    public void setTarget(final BasicBlock branch) {
        target = NodeHandle.of(branch);
    }

    void setTarget(final NodeHandle target) {
        this.target = target;
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public String getLabelForGraph() {
        return "goto";
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(target), "goes-to", "black", "solid", knownBlocks);
    }
}
