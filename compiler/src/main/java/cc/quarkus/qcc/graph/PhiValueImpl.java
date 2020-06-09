package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.constraint.Assert;

final class PhiValueImpl extends ValueProgramNodeImpl implements PhiValue {
    private final Key key = new Key();
    private NodeHandle type;

    PhiValueImpl() {
    }

    public Value getValueForBlock(final BasicBlock input) {
        NodeHandle handle = ((BasicBlockImpl) input).outboundValues.get(key);
        return handle == null ? null : NodeHandle.getTargetOf(handle);
    }

    public void setValueForBlock(final BasicBlock input, final Value value) {
        Assert.checkNotNullParam("value", value);
        if (value.getType() != getType()) {
            throw new IllegalStateException("Phi type mismatch");
        }
        BasicBlockImpl bbi = (BasicBlockImpl) input;
        Map<Key, NodeHandle> ov = bbi.outboundValues;
        if (ov.containsKey(key)) {
            throw new IllegalStateException("Phi " + this + " already has a value for block " + input);
        }
        bbi.outboundValues = Util.mapWithEntry(ov, key, NodeHandle.of(value));
    }

    public Type getType() {
        return NodeHandle.getTargetOf(type);
    }

    public void setType(final Type type) {
        this.type = NodeHandle.of(type);
    }

    public String getLabelForGraph() {
        return "phi";
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
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
    }

    static final class Key {}
}
