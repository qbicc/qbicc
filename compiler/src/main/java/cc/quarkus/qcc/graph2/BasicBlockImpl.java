package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 */
final class BasicBlockImpl extends NodeImpl implements BasicBlock {
    NodeHandle terminalInstruction;

    public TerminalInstruction getTerminalInstruction() {
        return terminalInstruction.getTarget();
    }

    public void setTerminalInstruction(final TerminalInstruction terminalInstruction) {
        this.terminalInstruction = NodeHandle.of(terminalInstruction);
    }

    public Set<BasicBlock> getReachableBlocks() {
        Set<BasicBlock> set = new LinkedHashSet<>();
        findReachable(set);
        return set;
    }

    private void findReachable(final Set<BasicBlock> set) {
        if (set.add(this)) {
            TerminalInstruction ti = terminalInstruction.getTarget();
            if (ti instanceof IfInstruction) {
                IfInstruction ifTi = (IfInstruction) ti;
                ((BasicBlockImpl)ifTi.getTrueBranch()).findReachable(set);
                ((BasicBlockImpl)ifTi.getFalseBranch()).findReachable(set);
            } else if (ti instanceof GotoInstruction) {
                ((BasicBlockImpl)((GotoInstruction) ti).getTarget()).findReachable(set);
            }
        }
    }


    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph) throws IOException {
        super.writeToGraph(visited, graph);
        // now write the edge to the terminal instruction
        addEdgeTo(visited, graph, terminalInstruction.getTarget(), "terminates-with", "black", "solid");
        return graph;
    }

    public String getLabelForGraph() {
        return "bblock";
    }

    String getShape() {
        return "trapezium";
    }
}
