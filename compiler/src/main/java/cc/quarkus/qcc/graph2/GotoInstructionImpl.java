package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
final class GotoInstructionImpl extends InstructionImpl implements GotoInstruction {
    NodeHandle target;

    GotoInstructionImpl() {
    }

    public BasicBlock getTarget() {
        return target.getTarget();
    }

    public void setTarget(final BasicBlock branch) {
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
        addEdgeTo(visited, graph, target.getTarget(), "goes-to", "black", "solid", knownBlocks);
        return graph;
    }
}
