package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

abstract class OwnedValueImpl extends ValueImpl implements OwnedValue {
    private NodeHandle owner;

    public BasicBlock getOwner() {
        return owner.getTarget();
    }

    public void setOwner(final BasicBlock owner) {
        this.owner = NodeHandle.of(owner);
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, getOwner(), "owned-by", "brown", "solid", knownBlocks);
        return graph;
    }
}
