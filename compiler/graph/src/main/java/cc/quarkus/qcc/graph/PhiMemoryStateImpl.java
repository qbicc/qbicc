package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.constraint.Assert;

final class PhiMemoryStateImpl extends MemoryStateImpl implements PhiMemoryState {
    private final Key key = new Key();
    private final NodeHandle basicBlock;

    PhiMemoryStateImpl(final NodeHandle basicBlock) {
        this.basicBlock = basicBlock;
    }

    PhiMemoryStateImpl(final BasicBlock basicBlock) {
        this(NodeHandle.of(basicBlock));
    }

    public MemoryState getMemoryStateForBlock(final BasicBlock input) {
        NodeHandle handle = ((BasicBlockImpl) input).outboundMemoryStates.get(key);
        return handle == null ? null : NodeHandle.getTargetOf(handle);
    }

    public void setMemoryStateForBlock(final BasicBlock input, final MemoryState memoryState) {
        Assert.checkNotNullParam("value", memoryState);
        BasicBlockImpl bbi = (BasicBlockImpl) input;
        Map<Key, NodeHandle> ov = bbi.outboundMemoryStates;
        if (ov.containsKey(key)) {
            throw new IllegalStateException("Phi " + this + " already has a memory state for block " + input);
        }
        bbi.outboundMemoryStates = Util.mapWithEntry(ov, key, NodeHandle.of(memoryState));
    }

    public String getLabelForGraph() {
        return "memory-phi";
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        int idx = 0;
        for (BasicBlock bb : knownBlocks) {
            MemoryState memoryState = getMemoryStateForBlock(bb);
            if (memoryState != null) {
                // this is pretty ugly
                addEdgeTo(visited, graph, bb, "phi-block#" + idx, "black", "solid", knownBlocks);
                addEdgeTo(visited, graph, memoryState, "phi-mem-state#" + idx, "black", "solid", knownBlocks);
                idx ++;
            }
        }
    }

    public BasicBlock getBasicBlock() {
        return NodeHandle.getTargetOf(basicBlock);
    }

    static final class Key {}
}
