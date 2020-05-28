package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
final class BasicBlockImpl extends NodeImpl implements BasicBlock {
    // used by phi nodes
    Map<PhiValueImpl.Key, NodeHandle> outboundValues = Map.of();
    NodeHandle terminalInstruction;

    public Terminator getTerminator() {
        return NodeHandle.getTargetOf(terminalInstruction);
    }

    public void setTerminator(final Terminator terminator) {
        this.terminalInstruction = NodeHandle.of(terminator);
    }

    public Set<BasicBlock> calculateReachableBlocks() {
        Set<BasicBlock> set = new LinkedHashSet<>();
        findReachable(set);
        return set;
    }

    private void findReachable(final Set<BasicBlock> set) {
        if (set.add(this)) {
            Terminator ti = NodeHandle.getTargetOf(terminalInstruction);
            if (ti instanceof If) {
                If ifTi = (If) ti;
                ((BasicBlockImpl)ifTi.getTrueBranch()).findReachable(set);
                ((BasicBlockImpl)ifTi.getFalseBranch()).findReachable(set);
            } else if (ti instanceof Goto) {
                ((BasicBlockImpl)((Goto) ti).getNextBlock()).findReachable(set);
            }
        }
    }


    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        // now write the edge to the terminal instruction
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(terminalInstruction), "terminates-with", "black", "solid", knownBlocks);
        return graph;
    }

    public String getLabelForGraph() {
        return "bblock";
    }

    String getShape() {
        return "trapezium";
    }
}
