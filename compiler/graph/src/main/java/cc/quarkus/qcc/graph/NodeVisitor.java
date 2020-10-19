package cc.quarkus.qcc.graph;

/**
 * A general visitor for the different node types.
 */
public interface NodeVisitor<T, R> {
    default R visitUnknown(T param, Node node) {
        return null;
    }

    default R visit(T param, Action node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Terminator node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Value node) {
        return visitUnknown(param, node);
    }
}
