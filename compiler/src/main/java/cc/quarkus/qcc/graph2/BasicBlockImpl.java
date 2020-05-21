package cc.quarkus.qcc.graph2;

import java.io.IOException;
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
