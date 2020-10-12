package cc.quarkus.qcc.graph;

/**
 * A visitor over a graph of terminator nodes.  Terminator nodes form a directed graph which may contain cycles.
 */
public interface TerminatorVisitor<T, R> {
    default R visitUnknown(T param, Terminator node) {
        return null;
    }

    default R visit(T param, Goto node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, If node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Jsr node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Ret node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Return node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Switch node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Throw node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Try node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ValueReturn node) {
        return visitUnknown(param, node);
    }

}
