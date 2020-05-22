package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

final class PhiInstructionImpl extends InstructionImpl implements PhiInstruction {
    private final PhiValue phiValue;

    PhiInstructionImpl(final PhiValue phiValue) {
        this.phiValue = phiValue;
    }

    public PhiValue getValue() {
        return phiValue;
    }

    public String getLabelForGraph() {
        return "inst:phi";
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, phiValue, "value", "yellow", "solid", knownBlocks);
        return graph;
    }
}
