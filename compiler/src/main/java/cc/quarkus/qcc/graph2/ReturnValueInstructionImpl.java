package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

final class ReturnValueInstructionImpl extends InstructionImpl implements ReturnValueInstruction {
    NodeHandle retVal;

    public String getLabelForGraph() {
        return "return";
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, retVal.getTarget(), "val", "black", "solid", knownBlocks);
        return graph;
    }

    public Value getReturnValue() {
        return retVal.getTarget();
    }

    public void setReturnValue(final Value value) {
        retVal = NodeHandle.of(value);
    }
}
