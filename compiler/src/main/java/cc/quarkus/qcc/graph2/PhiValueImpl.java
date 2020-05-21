package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

final class PhiValueImpl extends ValueImpl implements PhiValue {
    // specifically *not* using handles for map keys
    private Map<BasicBlock, NodeHandle> values = Map.of();

    public Value getValueForBlock(final BasicBlock input) {
        return values.get(input).getTarget();
    }

    public void setValueForBlock(final BasicBlock input, final Value value) {
        values = Util.copyMap(values, input, NodeHandle.of(value));
    }

    public String getLabelForGraph() {
        return "phi";
    }

    public Iterable<BasicBlock> getBlocks() {
        return values.keySet();
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph) throws IOException {
        super.writeToGraph(visited, graph);
        int idx = 0;
        for (BasicBlock bb : values.keySet()) {
            // this is pretty ugly
            addEdgeTo(visited, graph, bb, "phi-block#" + idx, "black", "solid");
            addEdgeTo(visited, graph, values.get(bb).getTarget(), "phi-value#" + idx, "black", "solid");
            idx ++;
        }
        return graph;
    }
}
