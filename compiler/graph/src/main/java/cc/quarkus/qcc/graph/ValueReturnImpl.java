package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

final class ValueReturnImpl extends TerminatorImpl implements ValueReturn {
    NodeHandle retVal;

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public String getLabelForGraph() {
        return "return";
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(retVal), "val", "black", "solid", knownBlocks);
    }

    public Value getReturnValue() {
        return NodeHandle.getTargetOf(retVal);
    }

    public void setReturnValue(final Value value) {
        retVal = NodeHandle.of(value);
    }
}
