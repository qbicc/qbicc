package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.constraint.Assert;

final class PhiDependencyImpl extends DependentNodeImpl implements PhiDependency {
    private final Key key = new Key();
    private final NodeHandle basicBlock;

    PhiDependencyImpl(final NodeHandle basicBlock) {
        this.basicBlock = basicBlock;
    }

    PhiDependencyImpl(final BasicBlock basicBlock) {
        this(NodeHandle.of(basicBlock));
    }

    public Node getDependencyForBlock(final BasicBlock input) {
        NodeHandle handle = ((BasicBlockImpl) input).outboundMemoryStates.get(key);
        return handle == null ? null : NodeHandle.getTargetOf(handle);
    }

    public void setDependencyForBlock(final BasicBlock input, final Node dependency) {
        Assert.checkNotNullParam("value", dependency);
        BasicBlockImpl bbi = (BasicBlockImpl) input;
        Map<Key, NodeHandle> ov = bbi.outboundMemoryStates;
        if (ov.containsKey(key)) {
            throw new IllegalStateException("Phi " + this + " already has a memory state for block " + input);
        }
        bbi.outboundMemoryStates = Util.mapWithEntry(ov, key, NodeHandle.of(dependency));
    }

    public void setDependencyForBlock(final NodeHandle input, final Node dependency) {
        setDependencyForBlock(NodeHandle.<BasicBlock>getTargetOf(input), dependency);
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public String getLabelForGraph() {
        return "memory-phi";
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        int idx = 0;
        for (BasicBlock bb : knownBlocks) {
            Node memoryState = getDependencyForBlock(bb);
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
