package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.constraint.Assert;

final class PhiValueImpl extends ProgramNodeImpl implements PhiValue {
    private final Key key = new Key();

    PhiValueImpl() {
    }

    public Value getValueForBlock(final BasicBlock input) {
        NodeHandle handle = ((BasicBlockImpl) input).outboundValues.get(key);
        return handle == null ? null : NodeHandle.getTargetOf(handle);
    }

    public void setValueForBlock(final BasicBlock input, final Value value) {
        Assert.checkNotNullParam("value", value);
        BasicBlockImpl bbi = (BasicBlockImpl) input;
        Map<Key, NodeHandle> ov = bbi.outboundValues;
        if (ov.containsKey(key)) {
            throw new IllegalStateException("Phi " + this + " already has a value for block " + input);
        }
        bbi.outboundValues = Util.copyMap(ov, key, NodeHandle.of(value));
    }

    public String getLabelForGraph() {
        return "phi";
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        int idx = 0;
        for (BasicBlock bb : knownBlocks) {
            Value val = getValueForBlock(bb);
            if (val != null) {
                // this is pretty ugly
                addEdgeTo(visited, graph, bb, "phi-block#" + idx, "black", "solid", knownBlocks);
                addEdgeTo(visited, graph, val, "phi-value#" + idx, "black", "solid", knownBlocks);
                idx ++;
            }
        }
        return graph;
    }

    static final class Key {}
}
