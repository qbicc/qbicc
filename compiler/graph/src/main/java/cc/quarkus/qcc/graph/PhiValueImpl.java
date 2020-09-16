package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.constraint.Constraint;
import io.smallrye.common.constraint.Assert;

final class PhiValueImpl extends ValueImpl implements PhiValue {
    private final Key key = new Key();
    private final NodeHandle basicBlock;
    private NodeHandle type;

    PhiValueImpl(final NodeHandle basicBlock) {
        this.basicBlock = basicBlock;
    }

    PhiValueImpl(final BasicBlock basicBlock) {
        this(NodeHandle.of(basicBlock));
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

    public void setValueForBlock(final NodeHandle input, final Value value) {
        setValueForBlock(NodeHandle.<BasicBlock>getTargetOf(input), value);
    }

    public Type getType() {
        return NodeHandle.getTargetOf(type);
    }

    public void setType(final Type type) {
        this.type = NodeHandle.of(type);
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
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

    public BasicBlock getBasicBlock() {
        return NodeHandle.getTargetOf(basicBlock);
    }

    public Constraint getConstraint() {
        return null;
    }

    public void setConstraint(final Constraint constraint) {

    }

    static final class Key {}
}
