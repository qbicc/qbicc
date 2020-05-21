package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
abstract class BinaryOpImpl extends ValueImpl implements BinaryOp {
    NodeHandle left;
    NodeHandle right;

    public Value getLeft() {
        return left.getTarget();
    }

    public void setLeft(final Value value) {
        left = NodeHandle.of(value);
    }

    public Value getRight() {
        return right.getTarget();
    }

    public void setRight(final Value value) {
        right = NodeHandle.of(value);
    }

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph) throws IOException {
        super.writeToGraph(visited, graph);
        addEdgeTo(visited, graph, left.getTarget(), "lhs", "black", "solid");
        addEdgeTo(visited, graph, right.getTarget(), "rhs", "black", "solid");
        return graph;
    }
}
